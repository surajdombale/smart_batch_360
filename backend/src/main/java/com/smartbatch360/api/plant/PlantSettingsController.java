package com.smartbatch360.api.plant;

import com.smartbatch360.api.plant.dto.PlantSettingsRequest;
import com.smartbatch360.api.plant.dto.PlantSettingsResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Settings > Plant Details. One row, so there is no id in the path. */
@RestController
@RequestMapping("/api/v1/plant-settings")
public class PlantSettingsController {

    private final PlantSettingsService plantSettingsService;

    public PlantSettingsController(PlantSettingsService plantSettingsService) {
        this.plantSettingsService = plantSettingsService;
    }

    @GetMapping
    public PlantSettingsResponse get() {
        return plantSettingsService.find();
    }

    @PutMapping
    public PlantSettingsResponse save(@Valid @RequestBody PlantSettingsRequest request) {
        return plantSettingsService.save(request);
    }
}
