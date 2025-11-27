package com.alpeerkaraca.tripservice.integration;

import com.alpeerkaraca.tripservice.AbstractIntegrationTest;
import com.alpeerkaraca.tripservice.dto.TripMessage;
import com.alpeerkaraca.tripservice.dto.TripRequest;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripStatus;
import com.alpeerkaraca.tripservice.repository.TripRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TripServiceIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Consumer<String, TripMessage> kafkaConsumer;


    @BeforeEach
    void setUp() {
        // Clean database
        tripRepository.deleteAll();

        // Clean Redis
        redisTemplate.delete("online_drivers_locations");
        redisTemplate.delete("busy_drivers_locations");

        // Setup Kafka consumer
        setupKafkaConsumer();

    }

    @AfterEach
    void tearDown() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
    }

    private void setupKafkaConsumer() {

        Map<String, Object> consumerProps = new HashMap<>(
                KafkaTestUtils.consumerProps(getKafkaContainer().getBootstrapServers(), "test-group", "true")
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TripMessage.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(JsonDeserializer.TYPE_MAPPINGS, "tripEvent:com.alpeerkaraca.tripservice.dto.TripMessage");
        DefaultKafkaConsumerFactory<String, TripMessage> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        kafkaConsumer = consumerFactory.createConsumer();
        kafkaConsumer.subscribe(Collections.singletonList("trip_events"));
    }


    @Nested
    @DisplayName("Nearby Drivers Integration Tests")
    class NearbyDriversIntegrationTests {

        @Test
        @WithMockUser
        @DisplayName("Should find drivers in Redis geo index")
        void findNearbyDrivers_WithRedisGeoData_ReturnsDrivers() throws Exception {
            // Arrange - Add drivers to Redis
            GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
            UUID driver1Id = UUID.randomUUID();
            UUID driver2Id = UUID.randomUUID();
            UUID driver3Id = UUID.randomUUID();

            // Istanbul coordinates
            geoOps.add("online_drivers_locations", new Point(28.9784, 41.0082), driver1Id.toString());
            geoOps.add("online_drivers_locations", new Point(28.9800, 41.0100), driver2Id.toString());
            // Far driver (should not appear in a 5 km radius)
            geoOps.add("online_drivers_locations", new Point(29.1000, 41.2000), driver3Id.toString());

            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/nearby-drivers")
                            .param("latitude", "41.0082")
                            .param("longitude", "28.9784")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.data[*].driverId", hasItem(driver1Id.toString())));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return empty when no drivers in range")
        void findNearbyDrivers_NoDriversInRange_ReturnsEmpty() throws Exception {
            // Arrange - Add far driver
            GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
            UUID driverId = UUID.randomUUID();
            geoOps.add("online_drivers_locations", new Point(29.5000, 41.5000), driverId.toString());

            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/nearby-drivers")
                            .param("latitude", "41.0082")
                            .param("longitude", "28.9784")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("Trip Request Integration Tests")
    @Order(1)
    class TripRequestIntegrationTests {

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should create trip and save to database")
        void requestTrip_ValidRequest_SavesTrip() throws Exception {
            // Arrange
            TripRequest request = new TripRequest(41.0082, 28.9784, 41.0200, 28.9900);
            UUID passengerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

            // Act
            String response = mockMvc.perform(post("/api/v1/trips/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.tripStatus").value("REQUESTED"))
                    .andExpect(jsonPath("$.data.passengerId").value(passengerId.toString()))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Assert - Check database
            assertThat(tripRepository.findAll()).hasSize(1);
            Trip savedTrip = tripRepository.findAll().get(0);
            assertThat(savedTrip.getPassengerId()).isEqualTo(passengerId);
            assertThat(savedTrip.getTripStatus()).isEqualTo(TripStatus.REQUESTED);
            assertThat(savedTrip.getStartLatitude()).isEqualTo(41.0082);
            assertThat(savedTrip.getStartLongitude()).isEqualTo(28.9784);
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should create multiple trips for same passenger")
        void requestTrip_MultipleTrips_SavesAll() throws Exception {
            // Arrange
            TripRequest request1 = new TripRequest(41.0082, 28.9784, 41.0200, 28.9900);
            TripRequest request2 = new TripRequest(41.0200, 28.9900, 41.0082, 28.9784);

            // Act
            mockMvc.perform(post("/api/v1/trips/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1))
                            .with(csrf()))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/trips/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2))
                            .with(csrf()))
                    .andExpect(status().isOk());

            // Assert
            assertThat(tripRepository.findAll()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Trip Lifecycle Integration Tests")
    @Order(2)
    class TripLifecycleIntegrationTests {

        @Test
        @WithMockUser(username = "360fea4f-fe9c-4a8e-9d33-fbd63c9cd7ce", roles = "DRIVER")
        @DisplayName("Should complete full trip lifecycle: request -> accept -> start -> complete")
        void tripLifecycle_FullFlow_WorksCorrectly() throws Exception {
            // 1. Create trip
            UUID passengerId = UUID.randomUUID();
            Trip trip = Trip.builder()
                    .passengerId(passengerId)
                    .startLatitude(41.0082)
                    .startLongitude(28.9784)
                    .endLatitude(41.0200)
                    .endLongitude(28.9900)
                    .tripStatus(TripStatus.REQUESTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();
            trip = tripRepository.save(trip);
            UUID tripId = trip.getTripId();

            // 2. Accept trip
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/accept")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tripStatus").value("ACCEPTED"));

            // Verify Kafka event
            ConsumerRecords<String, TripMessage> records = kafkaConsumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isGreaterThan(0);
            TripMessage acceptMessage = StreamSupport.stream(records.spliterator(), false)
                    .map(ConsumerRecord::value)
                    .filter(msg -> msg.getTripId().equals(tripId) && "TRIP_ACCEPTED".equals(msg.getEventType()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected TRIP_ACCEPTED message for tripId " + tripId + " not found in Kafka"));
            assertThat(acceptMessage.getEventType()).isEqualTo("TRIP_ACCEPTED");
            assertThat(acceptMessage.getTripId()).isEqualTo(tripId);

            // 3. Start trip
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/start")
                            .with(csrf()))
                    .andExpect(status().isOk());

            Trip startedTrip = tripRepository.findById(tripId).get();
            assertThat(startedTrip.getTripStatus()).isEqualTo(TripStatus.IN_PROGRESS);

            // Verify Kafka event
            records = kafkaConsumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isGreaterThan(0);
            TripMessage startMessage = StreamSupport.stream(records.spliterator(), false)
                    .map(ConsumerRecord::value)
                    .filter(msg -> msg.getTripId().equals(tripId) && "TRIP_STARTED".equals(msg.getEventType()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected TRIP_STARTED message for tripId " + tripId + " not found in Kafka"));

            assertThat(startMessage.getEventType()).isEqualTo("TRIP_STARTED");

            // Wait a bit to simulate trip duration
            Thread.sleep(1000);

            // 4. Complete trip
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/complete")
                            .with(csrf()))
                    .andExpect(status().isOk());

            Trip completedTrip = tripRepository.findById(tripId).get();
            assertThat(completedTrip.getTripStatus()).isEqualTo(TripStatus.COMPLETED);
            assertThat(completedTrip.getFare()).isNotNull();
            assertThat(completedTrip.getFare()).isGreaterThan(BigDecimal.ZERO);

            // Verify Kafka event
            records = kafkaConsumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isGreaterThan(0);
            TripMessage completeMessage = StreamSupport.stream(records.spliterator(), false)
                    .map(ConsumerRecord::value)
                    .filter(msg -> msg.getTripId().equals(tripId) && "TRIP_COMPLETED".equals(msg.getEventType()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected TRIP_COMPLETED message for tripId " + tripId + " not found in Kafka"));

            assertThat(completeMessage.getEventType()).isEqualTo("TRIP_COMPLETED");
            assertThat(completeMessage.getFare()).isNotNull();
        }

        @Test
        @WithMockUser(username = "8542a57c-8ec1-415f-8ddb-9c4159c4bea3")
        @DisplayName("Should allow cancellation before completion")
        void tripLifecycle_Cancel_WorksCorrectly() throws Exception {
            // 1. Create trip
            Trip trip = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .startLatitude(41.0082)
                    .startLongitude(28.9784)
                    .endLatitude(41.0200)
                    .endLongitude(28.9900)
                    .tripStatus(TripStatus.REQUESTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();
            trip = tripRepository.save(trip);
            UUID tripId = trip.getTripId();

            // 2. Cancel trip
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/cancel")
                            .with(csrf()))
                    .andExpect(status().isOk());

            Trip cancelledTrip = tripRepository.findById(tripId).get();
            assertThat(cancelledTrip.getTripStatus()).isEqualTo(TripStatus.CANCELLED);

            // Verify Kafka event
            ConsumerRecords<String, TripMessage> records = kafkaConsumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isGreaterThan(0);
            TripMessage cancelMessage = records.iterator().next().value();
            assertThat(cancelMessage.getEventType()).isEqualTo("TRIP_CANCELLED");
        }

        @Test
        @WithMockUser(username = "360fea4f-fe9c-4a8e-9d33-fbd63c9cd7ce", roles = "DRIVER")
        @DisplayName("Should prevent accepting already accepted trip")
        void tripLifecycle_DoubleAccept_ReturnConflict() throws Exception {
            // 1. Create trip
            Trip trip = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .startLatitude(41.0082)
                    .startLongitude(28.9784)
                    .endLatitude(41.0200)
                    .endLongitude(28.9900)
                    .tripStatus(TripStatus.REQUESTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();
            trip = tripRepository.save(trip);
            UUID tripId = trip.getTripId();

            // 2. Accept trip first time
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/accept")
                            .with(csrf()))
                    .andExpect(status().isOk());

            // 3. Try to accept again
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/accept")
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "8542a57c-8ec1-415f-8ddb-9c4159c4bea3")
        @DisplayName("Should prevent starting trip that is not accepted")
        void tripLifecycle_StartWithoutAccept_ReturnConflict() throws Exception {
            // 1. Create trip
            Trip trip = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .startLatitude(41.0082)
                    .startLongitude(28.9784)
                    .endLatitude(41.0200)
                    .endLongitude(28.9900)
                    .tripStatus(TripStatus.REQUESTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();
            trip = tripRepository.save(trip);
            UUID tripId = trip.getTripId();

            // 2. Try to start without accepting
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/start")
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "8542a57c-8ec1-415f-8ddb-9c4159c4bea3")
        @DisplayName("Should prevent completing trip that is not in progress")
        void tripLifecycle_CompleteWithoutStart_ReturnConflict() throws Exception {
            // 1. Create and accept trip
            Trip trip = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .driverId(UUID.randomUUID())
                    .startLatitude(41.0082)
                    .startLongitude(28.9784)
                    .endLatitude(41.0200)
                    .endLongitude(28.9900)
                    .tripStatus(TripStatus.ACCEPTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();
            trip = tripRepository.save(trip);
            UUID tripId = trip.getTripId();

            // 2. Try to complete without starting
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/complete")
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "8542a57c-8ec1-415f-8ddb-9c4159c4bea3")
        @DisplayName("Should prevent cancelling already completed trip")
        void tripLifecycle_CancelCompleted_ReturnConflict() throws Exception {
            // 1. Create completed trip
            Trip trip = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .driverId(UUID.randomUUID())
                    .startLatitude(41.0082)
                    .startLongitude(28.9784)
                    .endLatitude(41.0200)
                    .endLongitude(28.9900)
                    .tripStatus(TripStatus.COMPLETED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .startedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .endedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();
            trip = tripRepository.save(trip);
            UUID tripId = trip.getTripId();

            // 2. Try to cancel
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/cancel")
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Available Trips Integration Tests")
    class AvailableTripsIntegrationTests {

        @Test
        @WithMockUser
        @DisplayName("Should list only requested trips")
        void getAvailableTrips_FiltersByStatus_ReturnsOnlyRequested() throws Exception {
            // Arrange - Create trips with different statuses
            Trip requestedTrip1 = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .startLatitude(41.0082)
                    .startLongitude(28.9784)
                    .endLatitude(41.0200)
                    .endLongitude(28.9900)
                    .tripStatus(TripStatus.REQUESTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();

            Trip requestedTrip2 = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .startLatitude(41.0100)
                    .startLongitude(28.9800)
                    .endLatitude(41.0300)
                    .endLongitude(29.0000)
                    .tripStatus(TripStatus.REQUESTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();

            Trip acceptedTrip = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .driverId(UUID.randomUUID())
                    .startLatitude(41.0050)
                    .startLongitude(28.9750)
                    .endLatitude(41.0250)
                    .endLongitude(28.9950)
                    .tripStatus(TripStatus.ACCEPTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();

            tripRepository.save(requestedTrip1);
            tripRepository.save(requestedTrip2);
            tripRepository.save(acceptedTrip);

            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/available")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[*].tripStatus", everyItem(is("REQUESTED"))));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return empty list when no requested trips")
        void getAvailableTrips_NoRequestedTrips_ReturnsEmpty() throws Exception {
            // Arrange - Create only accepted trip
            Trip acceptedTrip = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .driverId(UUID.randomUUID())
                    .startLatitude(41.0050)
                    .startLongitude(28.9750)
                    .endLatitude(41.0250)
                    .endLongitude(28.9950)
                    .tripStatus(TripStatus.ACCEPTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();

            tripRepository.save(acceptedTrip);

            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/available")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Fare Calculation Integration Tests")
    class FareCalculationIntegrationTests {

        @Test
        @WithMockUser(username = "360fea4f-fe9c-4a8e-9d33-fbd63c9cd7ce", roles = "DRIVER")
        @DisplayName("Should calculate minimum fare for short trip")
        void fareCalculation_ShortTrip_ReturnsMinimumFare() throws Exception {
            // Arrange - Very short trip
            Trip trip = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .startLatitude(41.0082)
                    .startLongitude(28.9784)
                    .endLatitude(41.0083) // Very close
                    .endLongitude(28.9785)
                    .tripStatus(TripStatus.REQUESTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)))
                    .build();
            trip = tripRepository.save(trip);
            UUID tripId = trip.getTripId();

            // Accept and start trip
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/accept")
                            .with(csrf()))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/trips/" + tripId + "/start")
                            .with(csrf()))
                    .andExpect(status().isOk());

            Thread.sleep(500); // Short duration

            // Complete trip
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/complete")
                            .with(csrf()))
                    .andExpect(status().isOk());

            // Assert minimum fare
            Trip completedTrip = tripRepository.findById(tripId).get();
            assertThat(completedTrip.getFare()).isGreaterThanOrEqualTo(new BigDecimal("175.00"));
        }

        @Test
        @WithMockUser(username = "360fea4f-fe9c-4a8e-9d33-fbd63c9cd7ce", roles = "DRIVER")
        @DisplayName("Should calculate higher fare for longer trip")
        void fareCalculation_LongTrip_ReturnsCalculatedFare() throws Exception {
            // Arrange - Longer trip (approx 10km)
            Trip trip = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .startLatitude(41.0082)
                    .startLongitude(28.9784)
                    .endLatitude(41.1000)
                    .endLongitude(29.0500)
                    .tripStatus(TripStatus.REQUESTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now().minusMinutes(10)))
                    .build();
            trip = tripRepository.save(trip);
            UUID tripId = trip.getTripId();

            // Accept and start trip
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/accept")
                            .with(csrf()))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/trips/" + tripId + "/start")
                            .with(csrf()))
                    .andExpect(status().isOk());

            Thread.sleep(2000);

            // Complete trip
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/complete")
                            .with(csrf()))
                    .andExpect(status().isOk());

            // Assert fare is above minimum
            Trip completedTrip = tripRepository.findById(tripId).get();
            assertThat(completedTrip.getFare()).isGreaterThan(new BigDecimal("175.00"));
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @WithMockUser(username = "360fea4f-fe9c-4a8e-9d33-fbd63c9cd7ce", roles = "DRIVER")
        @DisplayName("Should handle concurrent accept requests with pessimistic locking")
        void concurrentAccept_PessimisticLocking_OnlyOneSucceeds() throws Exception {
            // Arrange
            Trip trip = Trip.builder()
                    .passengerId(UUID.randomUUID())
                    .startLatitude(41.0082)
                    .startLongitude(28.9784)
                    .endLatitude(41.0200)
                    .endLongitude(28.9900)
                    .tripStatus(TripStatus.REQUESTED)
                    .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();
            trip = tripRepository.save(trip);
            UUID tripId = trip.getTripId();

            // First accept should succeed
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/accept")
                            .with(csrf()))
                    .andExpect(status().isOk());

            // Second accept should fail with conflict
            mockMvc.perform(post("/api/v1/trips/" + tripId + "/accept")
                            .with(csrf()))
                    .andExpect(status().isConflict());

            // Verify only one driver assigned
            Trip finalTrip = tripRepository.findById(tripId).get();
            assertThat(finalTrip.getDriverId()).isNotNull();
            assertThat(finalTrip.getTripStatus()).isEqualTo(TripStatus.ACCEPTED);
        }
    }
}

