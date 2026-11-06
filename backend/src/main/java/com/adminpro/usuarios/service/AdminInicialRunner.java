package com.adminpro.usuarios.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.adminpro.security.PasswordPolicy;
import com.adminpro.usuarios.entity.Rol;
import com.adminpro.usuarios.entity.Usuario;
import com.adminpro.usuarios.repository.RolRepository;
import com.adminpro.usuarios.repository.UsuarioRepository;

/**
 * En producción no hay datos de prueba, y sin un primer Administrador nadie podría entrar.
 * Se crea desde variables de entorno y debe cambiar su contraseña en el primer ingreso.
 */
@Component
public class AdminInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder codificador;
    private final String correo;
    private final String password;

    public AdminInicialRunner(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder codificador,
            @Value("${adminpro.admin-inicial.correo}") String correo,
            @Value("${adminpro.admin-inicial.password}") String password) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.codificador = codificador;
        this.correo = correo;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (usuarios.countByRol_CodigoAndActivoTrue(Rol.ADMINISTRADOR) > 0) {
            return;
        }
        if (correo.isBlank() || password.length() < 10 || !PasswordPolicy.cabeEnBcrypt(password)) {
            log.warn("No hay ningún Administrador activo. Define ADMIN_INICIAL_CORREO y "
                    + "ADMIN_INICIAL_PASSWORD (mínimo 10 caracteres) para crearlo.");
            return;
        }
        Rol administrador = roles.findByCodigo(Rol.ADMINISTRADOR).orElseThrow();
        usuarios.save(new Usuario(correo, codificador.encode(password), "Administrador", administrador, null));
        log.info("Administrador inicial creado");
    }
}
