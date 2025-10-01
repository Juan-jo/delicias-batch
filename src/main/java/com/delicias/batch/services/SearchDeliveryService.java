package com.delicias.batch.services;

import com.delicias.batch.dto.RespSearchDeliveryDTO;
import com.delicias.batch.exceptions.DuplicateAssignOrderException;
import com.delicias.batch.exceptions.RollbackTransactionException;
import com.delicias.batch.models.Deliveryman;
import com.delicias.batch.models.OrderView;
import com.delicias.batch.repository.DeliverymanRepository;
import com.delicias.soft.services.core.common.OrderStatus;
import com.delicias.soft.services.core.supabase.exception.SupabaseOrderDeliveryIdAreNotExistsException;
import com.delicias.soft.services.core.supabase.order.service.CoreSupabaseOrderService;
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

    private final CoreSupabaseOrderService coreSupabaseOrderService;
    private final DeliverymanRepository deliverymanRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Integer _15km = 15000;

    private final String sqlUpdatePosOrderStatus = "UPDATE pos_order SET status = ? WHERE id = ?";


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
                break;
            }
            catch (RollbackTransactionException e) {
                System.err.println("Delivery are not available: " + e.getMessage());
            }
        }

        return response;
    }


    public void whenStatusIsAssignedOrders(OrderView order) {

        List<Deliveryman> delivers = getDeliverersWithAssignedOrders(order);

        for (Deliveryman deliver: delivers) {

            try {

                assignOrderWhenAssignedOrder(order, deliver);
                break;
            }
            catch (DuplicateAssignOrderException e) {
                break;
            }
            catch (RollbackTransactionException e) {
                System.err.println("Delivery are not available: " + e.getMessage());
            }
        }
    }

    @Transactional
    private void assignOderWhenAvailable(OrderView orderDTO, Deliveryman deliver) {

        try {

            coreSupabaseOrderService.assignDeliveryUUID(orderDTO.getId(), deliver.getDeliveryUUID());

            jdbcTemplate.execute(String.format(
                    "SELECT public.assign_order_when_available(%s, '%s'::uuid, %s);",
                    deliver.getId(),
                    deliver.getDeliveryUUID(),
                    orderDTO.getId()
            ));

            jdbcTemplate.update(
                    sqlUpdatePosOrderStatus,
                    OrderStatus.DELIVERY_ASSIGNED_ORDER.name(),
                    orderDTO.getId()
            );

            System.out.println("\uD83D\uDE00 Assigned Delivery Order: " + orderDTO.getId());
        }
        catch (DuplicateKeyException d)  {
            throw new DuplicateAssignOrderException("Duplicate insert order");
        }
        catch (SupabaseOrderDeliveryIdAreNotExistsException e) {
            System.err.println("Error In Supabase Deliveres: " + e.getMessage());
            throw new RollbackTransactionException(String.format("Rollback assign deliverer %s", deliver.getDeliveryUUID()));
        }
        catch (DataAccessException e) {

            throw new RollbackTransactionException(String.format("Rollback assign deliverer %s", deliver.getDeliveryUUID()));
        }
    }

    @Transactional
    private void assignOrderWhenAssignedOrder(OrderView orderDTO, Deliveryman deliver) {

        try {

            coreSupabaseOrderService.assignDeliveryUUID(orderDTO.getId(), deliver.getDeliveryUUID());

            jdbcTemplate.execute(String.format(
                    "SELECT public.assign_order_when_delivery_assigned_orders('%s'::uuid, %s);",
                    deliver.getDeliveryUUID(),
                    orderDTO.getId()
            ));

            jdbcTemplate.update(
                    sqlUpdatePosOrderStatus,
                    OrderStatus.DELIVERY_ASSIGNED_ORDER.name(),
                    orderDTO.getId()
            );

            System.out.println("\uD83D\uDE00 Assigned Delivery Order: " + orderDTO.getId());

        }
        catch (DuplicateKeyException d)  {
            throw new DuplicateAssignOrderException("Duplicate insert order");
        }
        catch (SupabaseOrderDeliveryIdAreNotExistsException e) {
            throw new RollbackTransactionException(String.format("Error In Supabase Deliveres: %s", e.getMessage()));
        }
        catch (DataAccessException e) {

            throw new RollbackTransactionException(String.format("Rollback assign deliverer %s ----- %s", deliver.getDeliveryUUID(), e.getMessage()));
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
