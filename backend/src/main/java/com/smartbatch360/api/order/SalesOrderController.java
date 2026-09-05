package com.smartbatch360.api.order;

import com.smartbatch360.api.order.dto.SalesOrderRequest;
import com.smartbatch360.api.order.dto.SalesOrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @GetMapping
    public List<SalesOrderResponse> list() {
        return salesOrderService.findAll();
    }

    @GetMapping("/{id}")
    public SalesOrderResponse get(@PathVariable Long id) {
        return salesOrderService.findById(id);
    }

    @PostMapping
    public ResponseEntity<SalesOrderResponse> create(@Valid @RequestBody SalesOrderRequest request) {
        SalesOrderResponse created = salesOrderService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + created.id())).body(created);
    }

    @PostMapping("/{id}/start")
    public SalesOrderResponse start(@PathVariable Long id) {
        return salesOrderService.start(id);
    }

    @PostMapping("/{id}/fulfil")
    public SalesOrderResponse fulfil(@PathVariable Long id) {
        return salesOrderService.fulfil(id);
    }

    @PostMapping("/{id}/cancel")
    public SalesOrderResponse cancel(@PathVariable Long id) {
        return salesOrderService.cancel(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesOrderService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
