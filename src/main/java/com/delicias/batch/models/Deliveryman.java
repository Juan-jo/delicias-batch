package com.delicias.batch.models;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Entity
@Table(name = "deliverers")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Deliveryman {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "deliverers_id_seq")
    @SequenceGenerator(
            name = "deliverers_id_seq",
            allocationSize = 1
    )
    private Integer id;


    @Enumerated(EnumType.STRING)
    DeliverStatus status;

    @Column(name = "deliverer_id")
    private UUID deliveryUUID;


    @Column(name = "last_position", columnDefinition = "GEOGRAPHY(Point, 4326)")
    private Point lastPosition;

    @Column(name = "destination_position", columnDefinition = "GEOGRAPHY(Point, 4326)")
    private Point destinationPosition;



    public void updateLastPosition(double longitude, double latitude) {
        GeometryFactory geometryFactory = new GeometryFactory();
        this.lastPosition = geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }
}
