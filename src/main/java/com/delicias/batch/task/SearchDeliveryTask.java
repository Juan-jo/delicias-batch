package com.delicias.batch.task;

import com.delicias.batch.dto.RespSearchDeliveryDTO;
import com.delicias.batch.models.OrderView;
import com.delicias.batch.repository.OrderViewRepository;
import com.delicias.batch.services.SearchDeliveryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Component
public class SearchDeliveryTask {


    private final OrderViewRepository orderViewRepository;
    private final SearchDeliveryService searchDeliveryService;


    @Scheduled(fixedDelay = 15000)
    public void runEvery15Seconds() {

        List<OrderView> orders = orderViewRepository.findAll();

        for (OrderView order: orders) {

            log.info("Init Search Delivery Order {}", order.getId());

            RespSearchDeliveryDTO respSearchDeliveryDTO = searchDeliveryService.whenStatusIsAvailable(order);

            if(!respSearchDeliveryDTO.found()) {
                searchDeliveryService.whenStatusIsAssignedOrders(order);
            }

        }

    }

}
