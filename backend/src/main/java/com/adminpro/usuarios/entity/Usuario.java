package com.adminpro.usuarios.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.OptimisticLock;

import com.adminpro.common.BaseEntity;

@Entity
@Table(name = "usuarios")
public class Usuario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "nombre_visible", nullable = false, length = 120)
    private String nombreVisible;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private EmpleadoRef empleado;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = true;

    // Estos tres campos cambian con cada login. Se excluyen del control de versión para
    // que un inicio de sesión no invalide la edición que un Administrador tiene abierta.
    @OptimisticLock(excluded = true)
    @Column(name = "intentos_fallidos", nullable = false)
    private short intentosFallidos;

    @OptimisticLock(excluded = true)
    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @OptimisticLock(excluded = true)
    @Column(name = "ultimo_acceso_at")
    private Instant ultimoAccesoAt;

    @Version
    private long version;

    protected Usuario() {
    }

    public Usuario(String correo, String passwordHash, String nombreVisible, Rol rol, EmpleadoRef empleado) {
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.nombreVisible = nombreVisible;
        this.rol = rol;
        this.empleado = empleado;
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /** Devuelve true si este fallo bloqueó la cuenta. */
    public boolean registrarFallo(int maxIntentos, Instant bloqueoHasta) {
        intentosFallidos++;
        if (intentosFallidos < maxIntentos) {
            return false;
        }
        intentosFallidos = 0;
        bloqueadoHasta = bloqueoHasta;
        return true;
    }

    public void registrarAcceso(Instant ahora) {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        ultimoAccesoAt = ahora;
    }

    public void actualizar(String nombreVisible, Rol rol, EmpleadoRef empleado) {
        this.nombreVisible = nombreVisible;
        this.rol = rol;
        this.empleado = empleado;
    }

    public void asignarPasswordTemporal(String hash) {
        passwordHash = hash;
        debeCambiarPassword = true;
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public void cambiarPassword(String hash) {
        passwordHash = hash;
        debeCambiarPassword = false;
    }

    public void activar() {
        activo = true;
    }

    public void desactivar() {
        activo = false;
    }

    public Long getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public Rol getRol() {
        return rol;
    }

    public EmpleadoRef getEmpleado() {
        return empleado;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public Instant getUltimoAccesoAt() {
        return ultimoAccesoAt;
    }

    public long getVersion() {
        return version;
    }
}
