package com.delicias.batch.services;

import com.delicias.batch.dto.RespSearchDeliveryDTO;
import com.delicias.batch.exceptions.DuplicateAssignOrderException;
import com.delicias.batch.exceptions.RollbackTransactionException;
import com.delicias.batch.models.Deliveryman;
import com.delicias.batch.models.OrderView;
import com.delicias.batch.repository.DeliverymanRepository;
import com.delicias.soft.services.core.common.OrderStatus;
import com.delicias.soft.services.core.supabase.deliverer.dto.SupabaseDelivererDTO;
import com.delicias.soft.services.core.supabase.deliverer.service.CoreSupabaseDelivererService;
import com.delicias.soft.services.core.supabase.exception.SupabaseOrderDeliveryIdAreNotExistsException;
import com.delicias.soft.services.core.supabase.order.service.CoreSupabaseOrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class SearchDeliveryService {

    private final CoreSupabaseOrderService coreSupabaseOrderService;
    private final CoreSupabaseDelivererService coreSupabaseDelivererService;

    private final DeliverymanRepository deliverymanRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Integer _15km = 15000;

    private final String sqlUpdatePosOrderStatus = "UPDATE pos_order SET status = ? WHERE id = ?";


    public RespSearchDeliveryDTO whenStatusIsAvailable(OrderView order) {

        RespSearchDeliveryDTO response = new RespSearchDeliveryDTO(false, null);

        List<Deliveryman> delivers = getDeliversAvailable(order);

        for (Deliveryman deliver: delivers) {

            log.warn("Evaluate With Delivery {} Of Order: {} ", deliver.getDeliveryUUID(), order.getId());

            try {
                assignOderWhenAvailable(order, deliver);
                response = new RespSearchDeliveryDTO(true, deliver);
                break;
            }
            catch (DuplicateAssignOrderException e) {
                response = new RespSearchDeliveryDTO(true, null);
                break;
            }
            catch (SupabaseOrderDeliveryIdAreNotExistsException | RollbackTransactionException e) {
                log.info("Continue Evaluate the next Delivery. {}", e.getMessage());
            }
        }

        return response;
    }


    public RespSearchDeliveryDTO whenStatusIsAssignedOrders(OrderView order) {

        RespSearchDeliveryDTO response = new RespSearchDeliveryDTO(false, null);

        List<Deliveryman> delivers = getDeliverersWithAssignedOrders(order);

        for (Deliveryman deliver: delivers) {

            try {

                log.warn("Evaluate With Delivery {} Of Order: {} ", deliver.getDeliveryUUID(), order.getId());

                assignOrderWhenAssignedOrder(order, deliver);
                response = new RespSearchDeliveryDTO(true, deliver);
                break;
            }
            catch (DuplicateAssignOrderException e) {
                response = new RespSearchDeliveryDTO(true, null);
                break;
            }
            catch (SupabaseOrderDeliveryIdAreNotExistsException | RollbackTransactionException e) {
                log.info("Continue Evaluate the next Delivery. {}", e.getMessage());
            }
        }

        return response;
    }

    @Transactional
    private void assignOderWhenAvailable(OrderView orderDTO, Deliveryman deliver) {

        try {

            SupabaseDelivererDTO supabaseDelivererDTO = coreSupabaseDelivererService.getDeliverer(deliver.getDeliveryUUID());

            Optional.ofNullable(supabaseDelivererDTO).ifPresentOrElse(supabaseDeliver -> {

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

            }, () -> {

                throw new SupabaseOrderDeliveryIdAreNotExistsException(String.format("*2 - Supabase Delivery Are Not Exists: %s", deliver.getDeliveryUUID()));

            });

        }
        catch (DuplicateKeyException d)  {
            throw new DuplicateAssignOrderException("*3 - Duplicate insert order");
        }
        catch (DataAccessException e) {

            throw new RollbackTransactionException(String.format("*3 - Rollback assign deliverer %s", deliver.getDeliveryUUID()));
        }
    }

    @Transactional
    private void assignOrderWhenAssignedOrder(OrderView orderDTO, Deliveryman deliver) {

        try {

            SupabaseDelivererDTO supabaseDelivererDTO = coreSupabaseDelivererService.getDeliverer(deliver.getDeliveryUUID());

            Optional.ofNullable(supabaseDelivererDTO).ifPresentOrElse(supabaseDeliver -> {

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

            }, () -> {

                throw new SupabaseOrderDeliveryIdAreNotExistsException(String.format("*2 - Supabase Delivery Are Not Exists: %s", deliver.getDeliveryUUID()));
            });

        }
        catch (DuplicateKeyException d)  {
            throw new DuplicateAssignOrderException("Duplicate insert order");
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
