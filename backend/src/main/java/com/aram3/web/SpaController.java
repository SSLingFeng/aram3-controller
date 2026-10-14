package com.aram3.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA fallback: non-API routes return the Vue index.html so client-side router works.
 */
@Controller
public class SpaController {

    @GetMapping(value = {
            "/",
            "/login",
            "/instances",
            "/maps",
            "/mods",
            "/instance-mods",
            "/instance-config",
            "/mod-presets",
            "/jobs",
            "/steamcmd",
            "/users",
            "/settings"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
