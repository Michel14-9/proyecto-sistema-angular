package com.sistemaapolloAngular.sistema_apolloAngular.service;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Direccion;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.DireccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
public class DireccionService {

    private static final Logger logger = LoggerFactory.getLogger(DireccionService.class);

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private UsuarioService usuarioService;

    // OBTENER DIRECCIONES DEL USUARIO
    public List<Direccion> obtenerDireccionesUsuario(String username) {
        Preconditions.checkArgument(StringUtils.isNotBlank(username), "El nombre de usuario no puede estar vacío");
        logger.info("Obteniendo direcciones del usuario: {}", username);

        Usuario usuario = usuarioService.buscarPorCorreo(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return direccionRepository.findByUsuario(usuario);
    }

    // OBTENER DIRECCIÓN POR ID
    public Optional<Direccion> obtenerDireccionPorId(Long id) {
        Preconditions.checkNotNull(id, "El ID de la dirección no puede ser nulo");
        logger.debug("Buscando dirección con ID: {}", id);
        return direccionRepository.findById(id);
    }

    // GUARDAR NUEVA DIRECCIÓN
    public Direccion guardarDireccion(Direccion direccion, String username) {
        Preconditions.checkNotNull(direccion, "La dirección no puede ser nula");
        Preconditions.checkArgument(StringUtils.isNotBlank(username), "El nombre de usuario no puede estar vacío");
        logger.info("Guardando nueva dirección para usuario: {}", username);

        Usuario usuario = usuarioService.buscarPorCorreo(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (direccion.isPredeterminada()) {
            logger.debug("Quitando direcciones predeterminadas previas para usuario: {}", usuario.getUsername());
            quitarPredeterminadas(usuario);
        }

        direccion.setUsuario(usuario);
        Direccion guardada = direccionRepository.save(direccion);
        logger.info("Dirección guardada con ID: {}", guardada.getId());
        return guardada;
    }

    // ACTUALIZAR DIRECCIÓN
    public Direccion actualizarDireccion(Long id, Direccion direccionActualizada, String username) {
        Preconditions.checkNotNull(id, "El ID no puede ser nulo");
        Preconditions.checkNotNull(direccionActualizada, "La dirección actualizada no puede ser nula");
        logger.info("Actualizando dirección ID: {} para usuario {}", id, username);

        Direccion direccion = direccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));

        if (!direccion.getUsuario().getUsername().equals(username)) {
            logger.warn("Intento de modificar dirección ajena por usuario: {}", username);
            throw new RuntimeException("No tienes permisos para editar esta dirección");
        }

        if (direccionActualizada.isPredeterminada()) {
            quitarPredeterminadas(direccion.getUsuario());
        }

        direccion.setNombre(direccionActualizada.getNombre());
        direccion.setTipo(direccionActualizada.getTipo());
        direccion.setDireccion(direccionActualizada.getDireccion());
        direccion.setReferencia(direccionActualizada.getReferencia());
        direccion.setCiudad(direccionActualizada.getCiudad());
        direccion.setTelefono(direccionActualizada.getTelefono());
        direccion.setPredeterminada(direccionActualizada.isPredeterminada());
        direccion.setFacturacion(direccionActualizada.isFacturacion());

        Direccion actualizada = direccionRepository.save(direccion);
        logger.info("Dirección actualizada correctamente: {}", actualizada.getId());
        return actualizada;
    }

    // ELIMINAR DIRECCIÓN
    public void eliminarDireccion(Long id, String username) {
        Preconditions.checkNotNull(id, "El ID no puede ser nulo");
        logger.info("Eliminando dirección ID: {} para usuario {}", id, username);

        Direccion direccion = direccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));

        if (!direccion.getUsuario().getUsername().equals(username)) {
            logger.warn("Intento de eliminar dirección ajena por usuario: {}", username);
            throw new RuntimeException("No tienes permisos para eliminar esta dirección");
        }

        direccionRepository.delete(direccion);
        logger.info("Dirección eliminada correctamente ID: {}", id);
    }

    // MARCAR DIRECCIÓN COMO PREDETERMINADA
    public void marcarPredeterminada(Long id, String username) {
        Preconditions.checkNotNull(id, "El ID no puede ser nulo");
        logger.info("Marcando dirección ID: {} como predeterminada para usuario {}", id, username);

        Usuario usuario = usuarioService.buscarPorCorreo(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Direccion direccion = direccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));

        if (!direccion.getUsuario().getUsername().equals(username)) {
            logger.warn("Intento no autorizado de marcar predeterminada por usuario: {}", username);
            throw new RuntimeException("No tienes permisos para modificar esta dirección");
        }

        quitarPredeterminadas(usuario);
        direccion.setPredeterminada(true);
        direccionRepository.save(direccion);
        logger.info("Dirección marcada como predeterminada ID: {}", id);
    }

    // OBTENER DIRECCIÓN PREDETERMINADA
    public Optional<Direccion> obtenerDireccionPredeterminada(String username) {
        Preconditions.checkArgument(StringUtils.isNotBlank(username), "El usuario no puede estar vacío");
        Usuario usuario = usuarioService.buscarPorCorreo(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return direccionRepository.findByUsuarioAndPredeterminadaTrue(usuario)
                .stream().findFirst();
    }

    // OBTENER DIRECCIÓN DE FACTURACIÓN
    public Optional<Direccion> obtenerDireccionFacturacion(String username) {
        Preconditions.checkArgument(StringUtils.isNotBlank(username), "El usuario no puede estar vacío");
        Usuario usuario = usuarioService.buscarPorCorreo(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return direccionRepository.findByUsuarioAndFacturacionTrue(usuario)
                .stream().findFirst();
    }


    private void quitarPredeterminadas(Usuario usuario) {
        List<Direccion> direcciones = direccionRepository.findByUsuarioAndPredeterminadaTrue(usuario);
        for (Direccion dir : direcciones) {
            dir.setPredeterminada(false);
            direccionRepository.save(dir);
        }
        logger.debug("Se eliminaron las direcciones predeterminadas anteriores del usuario {}", usuario.getUsername());
    }

    // VERIFICAR PROPIEDAD DE DIRECCIÓN
    public boolean verificarPropiedadDireccion(Long direccionId, String username) {
        Preconditions.checkNotNull(direccionId, "El ID de la dirección no puede ser nulo");
        return direccionRepository.findById(direccionId)
                .map(direccion -> direccion.getUsuario().getUsername().equals(username))
                .orElse(false);
    }
}

