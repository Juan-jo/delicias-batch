package com.delicias.batch.task;

import com.delicias.batch.dto.RespSearchDeliveryDTO;
import com.delicias.batch.models.OrderView;
import com.delicias.batch.repository.OrderViewRepository;
import com.delicias.batch.services.SearchDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class SearchDeliveryTask {


    private final OrderViewRepository orderViewRepository;
    private final SearchDeliveryService searchDeliveryService;
    private final ExecutorService assignmentExecutor;

    public SearchDeliveryTask(
            OrderViewRepository orderViewRepository,
            SearchDeliveryService searchDeliveryService,
            @Qualifier("assignmentExecutor") ExecutorService assignmentExecutor)
    {
        this.orderViewRepository = orderViewRepository;
        this.searchDeliveryService = searchDeliveryService;
        this.assignmentExecutor = assignmentExecutor;
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void runEvery15Seconds() {

        List<OrderView> orders = orderViewRepository.findAll();

        if (orders.isEmpty()) {
            log.info("No pending orders to process.");
            return;
        }

        orders.forEach(order -> {

            assignmentExecutor.submit(() -> {
                try {
                    log.info("Init Search Delivery Order {}", order.getId());

                    RespSearchDeliveryDTO resp = searchDeliveryService.assign(order);

                    if(resp.deliveryman() != null) {

                        log.info("Order {} assigned to delivery {}", order.getId(), resp.deliveryman().getDeliveryUUID());
                        // Send Push Notification
                    }

                } catch (Exception e) {

                    System.err.println("Error al procesar la orden " + order.getId() + ": " + e.getMessage());
                }
            });
        });

    }


}
