package com.ihm.myapp.entity;

import java.util.List;
public class DirectionObject {
    private List<RouteObject> routes;
    private String status;
    private String error_message;
    public DirectionObject(List<RouteObject> routes, String status, String error_message) {
        this.routes = routes;
        this.status = status;
        this.error_message = error_message;
    }
    public List<RouteObject> getRoutes() {
        return routes;
    }
    public String getStatus() {
        return status;
    }
    public String getError_message() {
        return error_message;
    }
}