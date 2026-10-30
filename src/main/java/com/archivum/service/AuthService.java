package com.archivum.service;

import com.archivum.model.RolUsuario;
import com.archivum.model.Usuario;
import com.archivum.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH = 16;
    private static final String HASH_PREFIX = "pbkdf2$";
    private static final SecureRandom RNG = new SecureRandom();

    private final UsuarioRepository repository;
    private Usuario usuarioActivo;

    public AuthService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void inicializarUsuarios() {
        if (repository.count() == 0) {
            System.out.println("Creando usuarios por defecto...");
            crearUsuarioInterno("admin", "admin", RolUsuario.ADMINISTRADOR);
            crearUsuarioInterno("invitado", "1234", RolUsuario.CONSULTOR);
        }
    }

    // ---------------- Login / sesión ----------------

    public boolean login(String username, String password) {
        if (username == null || password == null) return false;
        Optional<Usuario> opt = repository.findByUsername(username);
        if (opt.isEmpty()) {
            this.usuarioActivo = null;
            return false;
        }
        Usuario u = opt.get();
        if (!verificarPassword(password, u.getPassword())) {
            this.usuarioActivo = null;
            return false;
        }
        // Migración transparente: si era texto plano, ahora pasa a hash
        if (!esHash(u.getPassword())) {
            u.setPassword(hashearPassword(password));
            repository.save(u);
        }
        this.usuarioActivo = u;
        return true;
    }

    public void logout() { this.usuarioActivo = null; }
    public boolean isLoggedIn() { return usuarioActivo != null; }
    public boolean isAdministrador() {
        return usuarioActivo != null && usuarioActivo.getRol() == RolUsuario.ADMINISTRADOR;
    }
    public Usuario getUsuarioActivo() { return usuarioActivo; }

    public String getRolActual() {
        if (usuarioActivo == null) return "No autenticado";
        return switch (usuarioActivo.getRol()) {
            case ADMINISTRADOR -> "Administrador";
            case CONSULTOR -> "Consultor";
        };
    }

    public String getUsernameActual() {
        return usuarioActivo != null ? usuarioActivo.getUsername() : "Invitado";
    }



    // ---------------- Gestión de usuarios ----------------

    public List<Usuario> listarUsuarios() {
        return repository.findAll();
    }

    public boolean existeUsername(String username) {
        return username != null && repository.findByUsername(username).isPresent();
    }

    /** Solo un administrador autenticado puede crear usuarios. */
    public Usuario crearUsuario(String username, String password, RolUsuario rol) {
        if (!isLoggedIn() || !isAdministrador())
            throw new IllegalStateException("Solo un administrador puede crear usuarios.");
        return crearUsuarioInterno(username, password, rol);
    }

    /** Cambio de contraseña de cualquier usuario (solo admin). */
    public void cambiarPassword(UUID userId, String nuevaPassword) {
        if (!isLoggedIn() || !isAdministrador())
            throw new IllegalStateException("Solo un administrador puede cambiar contraseñas de otros usuarios.");
        if (nuevaPassword == null || nuevaPassword.length() < 4)
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres.");
        Usuario u = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        u.setPassword(hashearPassword(nuevaPassword));
        repository.save(u);
    }

    /** Cambio de la propia contraseña (cualquier usuario autenticado). */
    public void cambiarPasswordActual(String actual, String nueva) {
        if (usuarioActivo == null)
            throw new IllegalStateException("No hay sesión activa.");
        if (!verificarPassword(actual, usuarioActivo.getPassword()))
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        if (nueva == null || nueva.length() < 4)
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 4 caracteres.");
        usuarioActivo.setPassword(hashearPassword(nueva));
        repository.save(usuarioActivo);
    }

    public void eliminarUsuario(UUID userId) {
        if (!isLoggedIn() || !isAdministrador())
            throw new IllegalStateException("Solo un administrador puede eliminar usuarios.");
        if (usuarioActivo.getId().equals(userId))
            throw new IllegalArgumentException("No puedes eliminar tu propia cuenta.");
        Usuario u = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (u.getRol() == RolUsuario.ADMINISTRADOR) {
            long admins = repository.findAll().stream()
                    .filter(x -> x.getRol() == RolUsuario.ADMINISTRADOR).count();
            if (admins <= 1)
                throw new IllegalArgumentException("Debe existir al menos un administrador.");
        }
        repository.delete(u);
    }

    // ---------------- Internos ----------------

    private Usuario crearUsuarioInterno(String username, String password, RolUsuario rol) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío.");
        username = username.trim();
        if (username.length() < 3)
            throw new IllegalArgumentException("El nombre de usuario debe tener al menos 3 caracteres.");
        if (password == null || password.length() < 4)
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres.");
        if (rol == null)
            throw new IllegalArgumentException("Debe seleccionar un rol.");
        if (existeUsername(username))
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");

        Usuario u = new Usuario(username, hashearPassword(password), rol);
        return repository.save(u);
    }

    private boolean esHash(String stored) {
        return stored != null && stored.startsWith(HASH_PREFIX);
    }

    private String hashearPassword(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        RNG.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS);
        return HASH_PREFIX + PBKDF2_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private boolean verificarPassword(String password, String stored) {
        if (password == null || stored == null) return false;
        if (!esHash(stored)) {
            // Compatibilidad con contraseñas antiguas en texto plano
            return stored.equals(password);
        }
        try {
            String[] parts = stored.substring(HASH_PREFIX.length()).split("\\$");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
            return constantTimeEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLenBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLenBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Error al hashear contraseña", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }
}