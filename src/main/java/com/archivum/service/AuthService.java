package com.archivum.service;

import com.archivum.model.RolUsuario;
import com.archivum.model.Usuario;
import com.archivum.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository repository;
    private Usuario usuarioActivo;

    public AuthService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void inicializarUsuarios() {
        // Crear usuarios por defecto solo si la BD está vacía
        if (repository.count() == 0) {
            System.out.println("Creando usuarios por defecto...");
            repository.save(new Usuario("admin", "admin", RolUsuario.ADMINISTRADOR));
            repository.save(new Usuario("invitado", "1234", RolUsuario.CONSULTOR));
        }
    }

    public boolean login(String username, String password) {
        Optional<Usuario> usuarioOpt = repository.findByUsername(username);
        if (usuarioOpt.isPresent() && usuarioOpt.get().getPassword().equals(password)) {
            this.usuarioActivo = usuarioOpt.get();
            return true;
        }
        this.usuarioActivo = null;
        return false;
    }

    public void logout() {
        this.usuarioActivo = null;
    }

    public boolean isLoggedIn() {
        return usuarioActivo != null;
    }

    public boolean isAdministrador() {
        return usuarioActivo != null && usuarioActivo.getRol() == RolUsuario.ADMINISTRADOR;
    }

    /** @return El nombre del rol actual en formato legible */
    public String getRolActual() {
        if (usuarioActivo == null) return "No autenticado";
        return switch (usuarioActivo.getRol()) {
            case ADMINISTRADOR -> "Administrador";
            case CONSULTOR -> "Consultor";
        };
    }

    /** @return El username del usuario activo, o "Invitado" si no hay sesión */
    public String getUsernameActual() {
        return usuarioActivo != null ? usuarioActivo.getUsername() : "Invitado";
    }
}