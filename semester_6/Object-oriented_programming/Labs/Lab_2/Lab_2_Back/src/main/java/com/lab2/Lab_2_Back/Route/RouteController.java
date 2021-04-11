package com.lab2.Lab_2_Back.Route;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "api/routes")
public class RouteController {

    private final RouteService routeService;

    @Autowired
    public RouteController(RouteService routeService){
        this.routeService = routeService;
    }

    @GetMapping
    public List<Route> GetRoutes(){
        return routeService.GetRoutes();
    }

    @PostMapping
    public void registerNewRoute(@RequestBody Route route){
        routeService.addNewRoute(route);
    }

    @DeleteMapping(path="{routeId}")
    public void deleteRoute(@PathVariable("routeId") Long routeId){
        routeService.deleteRoute(routeId);
    }

    @PutMapping(path="{routeId}")
    public void updateRoute(
            @PathVariable("routeId") Long routeId,
            @RequestBody Route route){
        routeService.updateRoute(routeId, route);
    }
}