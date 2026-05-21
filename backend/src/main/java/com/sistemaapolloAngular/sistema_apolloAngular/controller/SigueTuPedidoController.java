package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/sigue-tu-pedido")
public class SigueTuPedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    /**
     * Muestra el formulario de búsqueda de pedidos
     */
    @GetMapping
    public String mostrarFormularioSeguimiento(Model model) {
        // Agregar atributos vacíos para evitar errores en Thymeleaf
        model.addAttribute("pedido", null);
        model.addAttribute("error", null);
        return "sigue-tu-pedido";
    }

    /**
     * Busca un pedido por número de pedido
     */
    @PostMapping("/buscar")
    public String buscarPedido(
            @RequestParam("numeroPedido") String numeroPedido,
            @RequestParam(value = "canalPedido", required = false, defaultValue = "WEB") String canalPedido,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Limpiar y formatear el número de pedido
            String numeroLimpio = numeroPedido.trim().toUpperCase();


            Optional<Pedido> pedidoOpt = pedidoRepository.findByNumeroPedido(numeroLimpio);


            if (!pedidoOpt.isPresent()) {
                pedidoOpt = pedidoRepository.findByNumero(numeroLimpio);
            }


            System.out.println("=== DEBUG SIGUE TU PEDIDO ===");
            System.out.println("Buscando: " + numeroLimpio);
            System.out.println("Encontrado: " + pedidoOpt.isPresent());

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();
                System.out.println("ID Pedido: " + pedido.getId());
                System.out.println("Número: " + pedido.getNumero());
                System.out.println("Número Pedido: " + pedido.getNumeroPedido());
                System.out.println("Estado: " + pedido.getEstado());
                System.out.println("Canal: " + pedido.getCanal());
            }
            System.out.println("=============================");

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();

                // Verificar que el canal coincida (si se especificó)
                if (canalPedido != null && !canalPedido.isEmpty() &&
                        !canalPedido.equals(pedido.getCanal())) {
                    model.addAttribute("error",
                            "El pedido " + numeroLimpio + " no fue realizado a través de " +
                                    (canalPedido.equals("WEB") ? "Página Web" : canalPedido));
                    model.addAttribute("pedido", null);
                } else {
                    model.addAttribute("pedido", pedido);
                    model.addAttribute("error", null);
                }
            } else {
                model.addAttribute("error",
                        "No se encontró ningún pedido con el número: " + numeroLimpio +
                                ". Verifica que el número sea correcto.");
                model.addAttribute("pedido", null);
            }

        } catch (Exception e) {
            System.err.println("Error en buscarPedido: " + e.getMessage());
            model.addAttribute("error",
                    "Error al buscar el pedido. Por favor, intenta nuevamente.");
            model.addAttribute("pedido", null);
        }

        return "sigue-tu-pedido";
    }

    /**
     * Endpoint alternativo para búsqueda por número simple (sin canal)
     */
    @PostMapping("/buscar-simple")
    public String buscarPedidoSimple(
            @RequestParam("numeroPedido") String numeroPedido,
            Model model) {

        return buscarPedido(numeroPedido, "WEB", model, null);
    }
}
