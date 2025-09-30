package com.delicias.batch.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;

@Entity
@Table(name = "ready_orders_for_delivery")
@Getter
public class OrderView {

    @Id
    private Integer id;

    private String status;

    @Column(name = "user_address", columnDefinition = "GEOGRAPHY(Point, 4326)")
    private Point userAddress;

    @Column(name = "date_order")
    private LocalDate dateOrder;

    @Column(name = "restaurant_address", columnDefinition = "GEOGRAPHY(Point, 4326)")
    private Point restaurantAddress;

}
