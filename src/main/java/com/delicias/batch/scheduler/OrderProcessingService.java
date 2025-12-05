package com.delicias.batch.scheduler;

import com.delicias.batch.dto.RespSearchDeliveryDTO;
import com.delicias.batch.models.OrderView;
import com.delicias.batch.repository.OrderViewRepository;
import com.delicias.batch.services.AssignDeliveryService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service
@Slf4j
public class OrderProcessingService {

    private final OrderViewRepository orderViewRepository;
    private final AssignDeliveryService assignDeliveryService;

    // Cola de trabajo
    private final BlockingQueue<OrderView> orderQueue = new LinkedBlockingQueue<>();
    private final Set<Integer> inQueueSet = ConcurrentHashMap.newKeySet();

    private final ExecutorService consumerExecutor;


    public OrderProcessingService(OrderViewRepository orderViewRepository,
                                  AssignDeliveryService assignDeliveryService,
                                  ExecutorService consumerExecutor) {
        this.orderViewRepository = orderViewRepository;
        this.assignDeliveryService = assignDeliveryService;
        this.consumerExecutor = consumerExecutor;
    }


    @Scheduled(fixedDelay = 5000)
    public void producer() {

        List<OrderView> orders = orderViewRepository.findAll();

        if (orders.isEmpty()) {
            return;
        }

        for (OrderView order : orders) {

            // Evita duplicados
            boolean addedToSet = inQueueSet.add(order.getId());
            if (!addedToSet) {
                continue;
            }

            // Agregar a la cola
            boolean added = orderQueue.offer(order);

            if (!added) {
                // Si falla, liberar del set
                inQueueSet.remove(order.getId());
            }
        }

        log.info("Orders inQueueSet {}", inQueueSet);
    }


    @PostConstruct
    public void startConsumers() {

        int THREADS = 4;
        System.out.println("Starting " + THREADS + " consumer threads...");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

        for (int i = 0; i < THREADS; i++) {

            consumerExecutor.submit(() -> {

                while (true) {

                    OrderView order = null;

                    try {
                        // Espera hasta que haya una orden en la cola
                        order = orderQueue.take();

                        log.info("START [{}] Order {}", LocalDateTime.now().format(fmt), order.getId());
                        RespSearchDeliveryDTO resp = assignDeliveryService.assign(order);

                        if(resp.deliveryman() != null) {

                            log.info("END [{}] Order {} assigned to delivery {}", LocalDateTime.now().format(fmt), order.getId(), resp.deliveryman().getDeliveryUUID());
                        }

                    } catch (Exception e) {
                        System.err.println("Error processing order: " + e.getMessage());

                    } finally {
                        System.out.println("Consumer size " + (long) orderQueue.size());

                        // Eliminar del set para permitir reprocesamiento futuro
                        if (order != null) {
                            inQueueSet.remove(order.getId());
                        }
                    }
                }
            });
        }
    }
}
