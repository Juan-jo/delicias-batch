package com.delicias.batch.repository;

import com.delicias.batch.models.Deliveryman;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DeliverymanRepository extends JpaRepository<Deliveryman, Integer> {


    @Query(value = """
            SELECT 		*,
            			ST_Distance(d.last_position ,ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS distance_meters
            FROM        deliverers d
            WHERE       ST_DWithin(last_position::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,:distance)
                        AND
                        d.status = 'AVAILABLE'::public.deliver_status_type
            ORDER BY    distance_meters ASC;
            
            """,nativeQuery = true)
    List<Deliveryman> findAvailable(@Param("latitude") double latitude,
                                    @Param("longitude") double longitude,
                                    @Param("distance") double distance);


    @Query(value = """
                SELECT 		    *,
                        		ST_Distance(d.last_position , d.destination_position) +
                        		ST_Distance(d.destination_position , ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)
                        		as total_meters
                       FROM 	deliverers d
                       WHERE 	ST_DWithin( last_position::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :distance )
                                AND
                                d.status = 'ASSIGNED_ORDERS'::public.deliver_status_type
                                AND
                                ST_Distance(d.last_position , d.destination_position) +
                        		ST_Distance(d.destination_position , ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) is not null
                       ORDER BY	total_meters asc;
        """, nativeQuery = true)
    List<Deliveryman> findWithAssignedOrders(
            @Param("latitude") double restaurantLatitude,
            @Param("longitude") double restaurantLongitude,
            @Param("distance") double distance
    );

}

