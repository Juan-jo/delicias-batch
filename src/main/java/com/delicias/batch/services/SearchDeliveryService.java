package com.delicias.batch.services;

import com.delicias.batch.dto.RespSearchDeliveryDTO;
import com.delicias.batch.exceptions.DuplicateAssignOrderException;
import com.delicias.batch.exceptions.RollbackTransactionException;
import com.delicias.batch.models.Deliveryman;
import com.delicias.batch.models.OrderView;
import com.delicias.batch.repository.DeliverymanRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@AllArgsConstructor
@Service
public class SearchDeliveryService {

    private final DeliverymanRepository deliverymanRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Integer _15km = 15000;


    public RespSearchDeliveryDTO whenStatusIsAvailable(OrderView order) {

        RespSearchDeliveryDTO response = new RespSearchDeliveryDTO(false, null);

        List<Deliveryman> delivers = getDeliversAvailable(order);

        for (Deliveryman deliver: delivers) {

            try {
                assignOderWhenAvailable(order, deliver);
                response = new RespSearchDeliveryDTO(true, deliver);
                break;
            }
            catch (DuplicateAssignOrderException e) {
                response = new RespSearchDeliveryDTO(true, null);
                System.err.println("The order is assigned: " + e.getMessage());
                break;
            }
            catch (RollbackTransactionException e) {
                System.err.println("Deliverer are not available: " + e.getMessage());
            }
        }

        return response;
    }


    public RespSearchDeliveryDTO whenStatusIsAssignedOrders(OrderView order) {

        RespSearchDeliveryDTO response = new RespSearchDeliveryDTO(false, null);
        List<Deliveryman> delivers = getDeliverersWithAssignedOrders(order);

        for (Deliveryman deliver: delivers) {

            try {

                assignOrderWhenAssignedOrder(order, deliver);
                response = new RespSearchDeliveryDTO(true, deliver);
                break;
            }
            catch (DuplicateAssignOrderException e) {
                response = new RespSearchDeliveryDTO(true, deliver);
                System.err.println("The order is assigned: " + e.getMessage());
                break;
            }
            catch (RollbackTransactionException e) {
                System.err.println("Deliverer are not available: " + e.getMessage());
            }
        }
        return response;
    }

    @Transactional
    private void assignOderWhenAvailable(OrderView orderDTO, Deliveryman deliver) {

        try {

            jdbcTemplate.execute(String.format(
                    "SELECT public.assign_order_when_available(%s, '%s'::uuid, %s);",
                    deliver.getId(),
                    deliver.getDeliveryUID(),
                    orderDTO.getId()
            ));

            System.out.println("\uD83D\uDE00 Assigned deliver order " + orderDTO.getId());

        }
        catch (DuplicateKeyException d)  {
            throw new DuplicateAssignOrderException("Duplicate insert order");
        }
        catch (DataAccessException e) {

            throw new RollbackTransactionException(String.format("Rollback assign deliverer %s", deliver.getDeliveryUID()));
        }
    }

    @Transactional
    private void assignOrderWhenAssignedOrder(OrderView orderDTO, Deliveryman deliver) {

        try {

            jdbcTemplate.execute(String.format(
                    "SELECT public.assign_order_when_delivery_assigned_orders('%s'::uuid, %s);",
                    deliver.getDeliveryUID(),
                    orderDTO.getId()
            ));

            System.out.println("\uD83D\uDE00 Assigned deliver order " + orderDTO.getId());

        }
        catch (DuplicateKeyException d)  {
            throw new DuplicateAssignOrderException("Duplicate insert order");
        }
        catch (DataAccessException e) {

            throw new RollbackTransactionException(String.format("Rollback assign deliverer %s ----- %s", deliver.getDeliveryUID(), e.getMessage()));
        }
    }


    private List<Deliveryman> getDeliversAvailable(OrderView order) {
        return deliverymanRepository.findAvailable(
                        order.getRestaurantAddress().getY(),
                        order.getRestaurantAddress().getX(),
                        _15km);
    }

    private List<Deliveryman> getDeliverersWithAssignedOrders(OrderView order) {

        return deliverymanRepository.findWithAssignedOrders(
                        order.getRestaurantAddress().getY(),
                        order.getRestaurantAddress().getX(),
                        _15km
                );
    }
}
