package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        try {
            if (status != null) {
                int statusCode = Integer.parseInt(status.toString());

                if (statusCode == HttpStatus.NOT_FOUND.value()) {
                    return "error/404";
                } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                    return "error/403";
                } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                    return "error/500";
                }
            }
        } catch (NumberFormatException e) {

            return "error/generic";
        }

        return "error/generic";
    }
}
