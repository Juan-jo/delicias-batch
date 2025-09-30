package com.delicias.batch.task;

import com.delicias.batch.dto.RespSearchDeliveryDTO;
import com.delicias.batch.models.OrderView;
import com.delicias.batch.repository.OrderViewRepository;
import com.delicias.batch.services.SearchDeliveryService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Component
public class SearchDeliveryTask {


    private final OrderViewRepository orderViewRepository;
    private final SearchDeliveryService searchDeliveryService;

    @Scheduled(cron = "${delicias.tasks.cron}")
    public void runEvery20Seconds() {

        List<OrderView> orders = orderViewRepository.findAll();

        for (OrderView order: orders) {


            RespSearchDeliveryDTO respSearchDeliveryDTO = searchDeliveryService.whenStatusIsAvailable(order);

            if(!respSearchDeliveryDTO.found()) {
                respSearchDeliveryDTO = searchDeliveryService.whenStatusIsAssignedOrders(order);
            }


            if(respSearchDeliveryDTO.found()) {

                System.out.println("Order success assigned");


                Optional.ofNullable(respSearchDeliveryDTO.deliveryman()).ifPresent(delivery -> {

                    //TODO Patch deliveryUUID  order in supabase

                });
            }
        }

    }

}
