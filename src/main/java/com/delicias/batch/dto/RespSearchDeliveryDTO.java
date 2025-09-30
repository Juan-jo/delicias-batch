package com.delicias.batch.dto;

import com.delicias.batch.models.Deliveryman;

public record RespSearchDeliveryDTO(
        boolean found,
        Deliveryman deliveryman
) {
}
