package com.alpeerkaraca.tripservice.controller;

import com.alpeerkaraca.common.exception.GlobalExceptionHandler;
import com.alpeerkaraca.tripservice.dto.TripRequest;
import com.alpeerkaraca.tripservice.factory.PricingStrategyFactory;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripOutbox;
import com.alpeerkaraca.tripservice.model.TripStatus;
import com.alpeerkaraca.tripservice.repository.TripOutboxRepository;
import com.alpeerkaraca.tripservice.repository.TripRepository;
import com.alpeerkaraca.tripservice.service.TripManagementService;
import com.alpeerkaraca.tripservice.service.TripRequestService;
import com.diffblue.cover.annotations.ManagedByDiffblue;
import com.diffblue.cover.annotations.MethodsUnderTest;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.FormLoginRequestBuilder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.LogoutRequestBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.StatusResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {TripsController.class, GlobalExceptionHandler.class})
@DisabledInAotMode
@ExtendWith(SpringExtension.class)
class TripsControllerDiffblueTest {
    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @MockitoBean
    private TripManagementService tripManagementService;

    @MockitoBean
    private TripRequestService tripRequestService;

    @Autowired
    private TripsController tripsController;

    /**
     * Test {@link TripsController#getNearbyDrivers(Double, Double)}.
     *
     * <ul>
     *   <li>Then content string {@code {"success":true,"message":"Nearby drivers
     *       listed.","data":[]}}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#getNearbyDrivers(Double, Double)}
     */
    @Test
    @DisplayName(
            "Test getNearbyDrivers(Double, Double); then content string '{\"success\":true,\"message\":\"Nearby drivers listed.\",\"data\":[]}'")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({
            "org.springframework.http.ResponseEntity TripsController.getNearbyDrivers(Double, Double)"
    })
    void testGetNearbyDrivers_thenContentStringSuccessTrueMessageNearbyDriversListedData()
            throws Exception {
        // Arrange
        when(tripRequestService.findNearbyDrivers(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new ArrayList<>());

        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get("/api/v1/trips/nearby-drivers")
                        .param("latitude", String.valueOf(10.0d))
                        .param("longitude", String.valueOf(10.0d));

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content()
                                .string("{\"success\":true,\"message\":\"Nearby drivers listed.\",\"data\":[]}"));
    }

    /**
     * Test {@link TripsController#getNearbyDrivers(Double, Double)}.
     *
     * <ul>
     *   <li>Then status {@link StatusResultMatchers#isInternalServerError()}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#getNearbyDrivers(Double, Double)}
     */
    @Test
    @DisplayName("Test getNearbyDrivers(Double, Double); then status isInternalServerError()")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({
            "org.springframework.http.ResponseEntity TripsController.getNearbyDrivers(Double, Double)"
    })
    void testGetNearbyDrivers_thenStatusIsInternalServerError() throws Exception {
        // Arrange
        when(tripRequestService.findNearbyDrivers(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new ArrayList<>());

        MockHttpServletRequestBuilder getResult =
                MockMvcRequestBuilders.get("/api/v1/trips/nearby-drivers");
        getResult.accept("Nearby drivers listed.");

        MockHttpServletRequestBuilder requestBuilder =
                getResult
                        .param("latitude", String.valueOf(10.0d))
                        .param("longitude", String.valueOf(10.0d));

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isInternalServerError());
    }

    /**
     * Test {@link TripsController#requestTrip(TripRequest)}.
     *
     * <p>Method under test: {@link TripsController#requestTrip(TripRequest)}
     */
    @Test
    @DisplayName("Test requestTrip(TripRequest)")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({
            "com.alpeerkaraca.common.dto.ApiResponse TripsController.requestTrip(TripRequest)"
    })
    void testRequestTrip() throws Exception {
        // Arrange
        MockHttpServletRequestBuilder contentTypeResult =
                MockMvcRequestBuilders.post("/api/v1/trips/request")
                        .contentType(MediaType.APPLICATION_JSON);

        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
        TripRequest tripRequest = new TripRequest(10.0d, 10.0d, 10.0d, 10.0d);
        String content = jsonMapper.writeValueAsString(tripRequest);

        MockHttpServletRequestBuilder requestBuilder = contentTypeResult.content(content);

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content()
                                .string(
                                        "{\"success\":false,\"message\":\"Cannot invoke \\\"org.springframework.security.core.Authentication.getName()\\\""
                                                + " because the return value of \\\"org.springframework.security.core.context.SecurityContext.getAuthentication"
                                                + "()\\\" is null\",\"data\":null}"));
    }

    /**
     * Test {@link TripsController#getAvailableTrips()}.
     *
     * <p>Method under test: {@link TripsController#getAvailableTrips()}
     */
    @Test
    @DisplayName("Test getAvailableTrips()")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.getAvailableTrips()"})
    void testGetAvailableTrips() throws Exception {
        // Arrange
        when(tripManagementService.getAvailableTrips()).thenReturn(new ArrayList<>());

        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get("/api/v1/trips/available");

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content()
                                .string("{\"success\":true,\"message\":\"Available trips listed.\",\"data\":[]}"));
    }

    /**
     * Test {@link TripsController#getAvailableTrips()}.
     *
     * <ul>
     *   <li>Given array of {@link String} with {@code Available trips listed.}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#getAvailableTrips()}
     */
    @Test
    @DisplayName("Test getAvailableTrips(); given array of String with 'Available trips listed.'")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.getAvailableTrips()"})
    void testGetAvailableTrips_givenArrayOfStringWithAvailableTripsListed() throws Exception {
        // Arrange
        when(tripManagementService.getAvailableTrips()).thenReturn(new ArrayList<>());

        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get("/api/v1/trips/available");
        requestBuilder.accept("Available trips listed.");

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isInternalServerError());
    }

    /**
     * Test {@link TripsController#getAvailableTrips()}.
     *
     * <ul>
     *   <li>Given {@link TripRepository} {@link TripRepository#findAvailableTrips()} return {@link
     *       ArrayList#ArrayList()}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#getAvailableTrips()}
     */
    @Test
    @DisplayName(
            "Test getAvailableTrips(); given TripRepository findAvailableTrips() return ArrayList()")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.getAvailableTrips()"})
    void testGetAvailableTrips_givenTripRepositoryFindAvailableTripsReturnArrayList()
            throws Exception {
        // Arrange
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get("/api/v1/trips/available");

        TripRepository tripsRepository = mock(TripRepository.class);
        when(tripsRepository.findAvailableTrips()).thenReturn(new ArrayList<>());
        TripOutboxRepository tripOutboxRepository = mock(TripOutboxRepository.class);
        JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        TripManagementService tripManagementService =
                new TripManagementService(
                        tripsRepository,
                        tripOutboxRepository,
                        objectMapper,
                        new PricingStrategyFactory(new ArrayList<>()));
        TripsController tripsController = new TripsController(null, tripManagementService);

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content()
                                .string("{\"success\":true,\"message\":\"Available trips listed.\",\"data\":[]}"));
    }

    /**
     * Test {@link TripsController#acceptTrip(UUID)}.
     *
     * <ul>
     *   <li>Given {@code true}.
     *   <li>When {@link MockMvcRequestBuilders#post(String, Object[])} {@code
     *       /api/v1/trips/{tripId}/accept} randomUUID secure {@code true}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#acceptTrip(UUID)}
     */
    @Test
    @DisplayName(
            "Test acceptTrip(UUID); given 'true'; when post(String, Object[]) '/api/v1/trips/{tripId}/accept' randomUUID secure 'true'")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"org.springframework.http.ResponseEntity TripsController.acceptTrip(UUID)"})
    void testAcceptTrip_givenTrue_whenPostApiV1TripsTripIdAcceptRandomUUIDSecureTrue()
            throws Exception {
        // Arrange
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/accept", UUID.randomUUID());
        requestBuilder.secure(true);

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content()
                                .string(
                                        "{\"success\":false,\"message\":\"Cannot invoke \\\"org.springframework.security.core.Authentication.getName()\\\""
                                                + " because the return value of \\\"org.springframework.security.core.context.SecurityContext.getAuthentication"
                                                + "()\\\" is null\",\"data\":null}"));
    }

    /**
     * Test {@link TripsController#acceptTrip(UUID)}.
     *
     * <ul>
     *   <li>When {@link MockMvcRequestBuilders#post(String, Object[])} {@code
     *       /api/v1/trips/{tripId}/accept} randomUUID.
     *   <li>Then content string a string.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#acceptTrip(UUID)}
     */
    @Test
    @DisplayName(
            "Test acceptTrip(UUID); when post(String, Object[]) '/api/v1/trips/{tripId}/accept' randomUUID; then content string a string")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"org.springframework.http.ResponseEntity TripsController.acceptTrip(UUID)"})
    void testAcceptTrip_whenPostApiV1TripsTripIdAcceptRandomUUID_thenContentStringAString()
            throws Exception {
        // Arrange
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/accept", UUID.randomUUID());

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content()
                                .string(
                                        "{\"success\":false,\"message\":\"Cannot invoke \\\"org.springframework.security.core.Authentication.getName()\\\""
                                                + " because the return value of \\\"org.springframework.security.core.context.SecurityContext.getAuthentication"
                                                + "()\\\" is null\",\"data\":null}"));
    }

    /**
     * Test {@link TripsController#startTrip(UUID)}.
     *
     * <ul>
     *   <li>Given array of {@link String} with {@code Trip started.}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#startTrip(UUID)}
     */
    @Test
    @DisplayName("Test startTrip(UUID); given array of String with 'Trip started.'")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.startTrip(UUID)"})
    void testStartTrip_givenArrayOfStringWithTripStarted() throws Exception {
        // Arrange
        doNothing().when(tripManagementService).startTrip(Mockito.<UUID>any());

        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/start", UUID.randomUUID());
        requestBuilder.accept("Trip started.");

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isInternalServerError());
    }

    /**
     * Test {@link TripsController#startTrip(UUID)}.
     *
     * <ul>
     *   <li>Given {@link Trip#Trip()} DriverId is randomUUID.
     *   <li>Then status four hundred nine.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#startTrip(UUID)}
     */
    @Test
    @DisplayName(
            "Test startTrip(UUID); given Trip() DriverId is randomUUID; then status four hundred nine")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.startTrip(UUID)"})
    void testStartTrip_givenTripDriverIdIsRandomUUID_thenStatusFourHundredNine() throws Exception {
        // Arrange
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/start", UUID.randomUUID());

        Trip trip = new Trip();
        trip.setDriverId(UUID.randomUUID());
        trip.setEndAddress("42 Main St");
        trip.setEndLatitude(10.0d);
        trip.setEndLongitude(10.0d);
        trip.setEndedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip.setFare(new BigDecimal("2.3"));
        trip.setPassengerId(UUID.randomUUID());
        trip.setRequestedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip.setStartAddress("42 Main St");
        trip.setStartLatitude(10.0d);
        trip.setStartLongitude(10.0d);
        trip.setStartedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip.setTripId(UUID.randomUUID());
        trip.setTripStatus(TripStatus.REQUESTED);
        Optional<Trip> ofResult = Optional.of(trip);

        TripRepository tripsRepository = mock(TripRepository.class);
        when(tripsRepository.findById(Mockito.<UUID>any())).thenReturn(ofResult);
        TripOutboxRepository tripOutboxRepository = mock(TripOutboxRepository.class);
        JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        TripManagementService tripManagementService =
                new TripManagementService(
                        tripsRepository,
                        tripOutboxRepository,
                        objectMapper,
                        new PricingStrategyFactory(new ArrayList<>()));
        TripsController tripsController = new TripsController(null, tripManagementService);

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .build()
                .perform(requestBuilder)
                .andExpect(status().is(409));
    }

    /**
     * Test {@link TripsController#startTrip(UUID)}.
     *
     * <ul>
     *   <li>When {@link MockMvcRequestBuilders#post(String, Object[])} {@code
     *       /api/v1/trips/{tripId}/start} randomUUID.
     *   <li>Then status {@link StatusResultMatchers#isOk()}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#startTrip(UUID)}
     */
    @Test
    @DisplayName(
            "Test startTrip(UUID); when post(String, Object[]) '/api/v1/trips/{tripId}/start' randomUUID; then status isOk()")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.startTrip(UUID)"})
    void testStartTrip_whenPostApiV1TripsTripIdStartRandomUUID_thenStatusIsOk() throws Exception {
        // Arrange
        doNothing().when(tripManagementService).startTrip(Mockito.<UUID>any());

        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/start", UUID.randomUUID());

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content().string("{\"success\":true,\"message\":\"Trip started.\",\"data\":null}"));
    }

    /**
     * Test {@link TripsController#completeTrip(UUID)}.
     *
     * <ul>
     *   <li>Given array of {@link String} with {@code Trip completed.}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#completeTrip(UUID)}
     */
    @Test
    @DisplayName("Test completeTrip(UUID); given array of String with 'Trip completed.'")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.completeTrip(UUID)"})
    void testCompleteTrip_givenArrayOfStringWithTripCompleted() throws Exception {
        // Arrange
        doNothing().when(tripManagementService).completeTrip(Mockito.<UUID>any());

        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/complete", UUID.randomUUID());
        requestBuilder.accept("Trip completed.");

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isInternalServerError());
    }

    /**
     * Test {@link TripsController#completeTrip(UUID)}.
     *
     * <ul>
     *   <li>Given {@link Trip#Trip()} DriverId is randomUUID.
     *   <li>Then status four hundred nine.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#completeTrip(UUID)}
     */
    @Test
    @DisplayName(
            "Test completeTrip(UUID); given Trip() DriverId is randomUUID; then status four hundred nine")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.completeTrip(UUID)"})
    void testCompleteTrip_givenTripDriverIdIsRandomUUID_thenStatusFourHundredNine() throws Exception {
        // Arrange
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/complete", UUID.randomUUID());

        Trip trip = new Trip();
        trip.setDriverId(UUID.randomUUID());
        trip.setEndAddress("42 Main St");
        trip.setEndLatitude(10.0d);
        trip.setEndLongitude(10.0d);
        trip.setEndedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip.setFare(new BigDecimal("2.3"));
        trip.setPassengerId(UUID.randomUUID());
        trip.setRequestedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip.setStartAddress("42 Main St");
        trip.setStartLatitude(10.0d);
        trip.setStartLongitude(10.0d);
        trip.setStartedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip.setTripId(UUID.randomUUID());
        trip.setTripStatus(TripStatus.REQUESTED);
        Optional<Trip> ofResult = Optional.of(trip);

        TripRepository tripsRepository = mock(TripRepository.class);
        when(tripsRepository.findById(Mockito.<UUID>any())).thenReturn(ofResult);
        TripOutboxRepository tripOutboxRepository = mock(TripOutboxRepository.class);
        JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        TripManagementService tripManagementService =
                new TripManagementService(
                        tripsRepository,
                        tripOutboxRepository,
                        objectMapper,
                        new PricingStrategyFactory(new ArrayList<>()));
        TripsController tripsController = new TripsController(null, tripManagementService);

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .build()
                .perform(requestBuilder)
                .andExpect(status().is(409));
    }

    /**
     * Test {@link TripsController#completeTrip(UUID)}.
     *
     * <ul>
     *   <li>When {@link MockMvcRequestBuilders#post(String, Object[])} {@code
     *       /api/v1/trips/{tripId}/complete} randomUUID.
     *   <li>Then status {@link StatusResultMatchers#isOk()}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#completeTrip(UUID)}
     */
    @Test
    @DisplayName(
            "Test completeTrip(UUID); when post(String, Object[]) '/api/v1/trips/{tripId}/complete' randomUUID; then status isOk()")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.completeTrip(UUID)"})
    void testCompleteTrip_whenPostApiV1TripsTripIdCompleteRandomUUID_thenStatusIsOk()
            throws Exception {
        // Arrange
        doNothing().when(tripManagementService).completeTrip(Mockito.<UUID>any());

        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/complete", UUID.randomUUID());

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content().string("{\"success\":true,\"message\":\"Trip completed.\",\"data\":null}"));
    }

    /**
     * Test {@link TripsController#cancelTrip(UUID)}.
     *
     * <ul>
     *   <li>Given {@code /api/v1/trips/{tripId}/cancel}.
     *   <li>When formLogin.
     *   <li>Then status {@link StatusResultMatchers#isNotFound()}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#cancelTrip(UUID)}
     */
    @Test
    @DisplayName(
            "Test cancelTrip(UUID); given '/api/v1/trips/{tripId}/cancel'; when formLogin; then status isNotFound()")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.cancelTrip(UUID)"})
    void testCancelTrip_givenApiV1TripsTripIdCancel_whenFormLogin_thenStatusIsNotFound()
            throws Exception {
        // Arrange
        doNothing().when(tripManagementService).cancelTrip(Mockito.<UUID>any());

        FormLoginRequestBuilder requestBuilder = SecurityMockMvcRequestBuilders.formLogin();

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isNotFound());
    }

    /**
     * Test {@link TripsController#cancelTrip(UUID)}.
     *
     * <ul>
     *   <li>Given array of {@link String} with {@code Trip cancelled.}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#cancelTrip(UUID)}
     */
    @Test
    @DisplayName("Test cancelTrip(UUID); given array of String with 'Trip cancelled.'")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.cancelTrip(UUID)"})
    void testCancelTrip_givenArrayOfStringWithTripCancelled() throws Exception {
        // Arrange
        doNothing().when(tripManagementService).cancelTrip(Mockito.<UUID>any());

        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/cancel", UUID.randomUUID());
        requestBuilder.accept("Trip cancelled.");

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isInternalServerError());
    }

    /**
     * Test {@link TripsController#cancelTrip(UUID)}.
     *
     * <ul>
     *   <li>Given {@link Trip#Trip()} DriverId is randomUUID.
     *   <li>Then status {@link StatusResultMatchers#isOk()}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#cancelTrip(UUID)}
     */
    @Test
    @DisplayName("Test cancelTrip(UUID); given Trip() DriverId is randomUUID; then status isOk()")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.cancelTrip(UUID)"})
    void testCancelTrip_givenTripDriverIdIsRandomUUID_thenStatusIsOk() throws Exception {
        // Arrange
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/cancel", UUID.randomUUID());

        Trip trip = new Trip();
        trip.setDriverId(UUID.randomUUID());
        trip.setEndAddress("42 Main St");
        trip.setEndLatitude(10.0d);
        trip.setEndLongitude(10.0d);
        trip.setEndedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip.setFare(new BigDecimal("2.3"));
        trip.setPassengerId(UUID.randomUUID());
        trip.setRequestedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip.setStartAddress("42 Main St");
        trip.setStartLatitude(10.0d);
        trip.setStartLongitude(10.0d);
        trip.setStartedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip.setTripId(UUID.randomUUID());
        trip.setTripStatus(TripStatus.REQUESTED);
        Optional<Trip> ofResult = Optional.of(trip);

        Trip trip2 = new Trip();
        trip2.setDriverId(UUID.randomUUID());
        trip2.setEndAddress("42 Main St");
        trip2.setEndLatitude(10.0d);
        trip2.setEndLongitude(10.0d);
        trip2.setEndedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip2.setFare(new BigDecimal("2.3"));
        trip2.setPassengerId(UUID.randomUUID());
        trip2.setRequestedAt(
                LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip2.setStartAddress("42 Main St");
        trip2.setStartLatitude(10.0d);
        trip2.setStartLongitude(10.0d);
        trip2.setStartedAt(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        trip2.setTripId(UUID.randomUUID());
        trip2.setTripStatus(TripStatus.REQUESTED);

        TripRepository tripsRepository = mock(TripRepository.class);
        when(tripsRepository.save(Mockito.<Trip>any())).thenReturn(trip2);
        when(tripsRepository.findById(Mockito.<UUID>any())).thenReturn(ofResult);

        TripOutbox tripOutbox = new TripOutbox();
        tripOutbox.setAggregateId("42");
        tripOutbox.setAggregateType("Aggregate Type");
        tripOutbox.setCreatedAt(
                LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        tripOutbox.setEventType("Event Type");
        tripOutbox.setId(UUID.randomUUID());
        tripOutbox.setPayload("Payload");
        tripOutbox.setProcessed(true);

        TripOutboxRepository tripOutboxRepository = mock(TripOutboxRepository.class);
        when(tripOutboxRepository.save(Mockito.<TripOutbox>any())).thenReturn(tripOutbox);
        JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        TripManagementService tripManagementService =
                new TripManagementService(
                        tripsRepository,
                        tripOutboxRepository,
                        objectMapper,
                        new PricingStrategyFactory(new ArrayList<>()));
        TripsController tripsController = new TripsController(null, tripManagementService);

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content().string("{\"success\":true,\"message\":\"Trip cancelled.\",\"data\":null}"));
    }

    /**
     * Test {@link TripsController#cancelTrip(UUID)}.
     *
     * <ul>
     *   <li>Then content string {@code {"success":false,"message":"No endpoint POST
     *       /logout.","data":null}}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#cancelTrip(UUID)}
     */
    @Test
    @DisplayName(
            "Test cancelTrip(UUID); then content string '{\"success\":false,\"message\":\"No endpoint POST /logout.\",\"data\":null}'")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.cancelTrip(UUID)"})
    void testCancelTrip_thenContentStringSuccessFalseMessageNoEndpointPostLogoutDataNull()
            throws Exception {
        // Arrange
        doNothing().when(tripManagementService).cancelTrip(Mockito.<UUID>any());

        LogoutRequestBuilder requestBuilder = SecurityMockMvcRequestBuilders.logout();

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content()
                                .string(
                                        "{\"success\":false,\"message\":\"No endpoint POST /logout.\",\"data\":null}"));
    }

    /**
     * Test {@link TripsController#cancelTrip(UUID)}.
     *
     * <ul>
     *   <li>When {@link MockMvcRequestBuilders#post(String, Object[])} {@code
     *       /api/v1/trips/{tripId}/cancel} randomUUID.
     *   <li>Then status {@link StatusResultMatchers#isOk()}.
     * </ul>
     *
     * <p>Method under test: {@link TripsController#cancelTrip(UUID)}
     */
    @Test
    @DisplayName(
            "Test cancelTrip(UUID); when post(String, Object[]) '/api/v1/trips/{tripId}/cancel' randomUUID; then status isOk()")
    @Tag("ContributionFromDiffblue")
    @ManagedByDiffblue
    @MethodsUnderTest({"com.alpeerkaraca.common.dto.ApiResponse TripsController.cancelTrip(UUID)"})
    void testCancelTrip_whenPostApiV1TripsTripIdCancelRandomUUID_thenStatusIsOk() throws Exception {
        // Arrange
        doNothing().when(tripManagementService).cancelTrip(Mockito.<UUID>any());

        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/api/v1/trips/{tripId}/cancel", UUID.randomUUID());

        // Act and Assert
        MockMvcBuilders.standaloneSetup(tripsController)
                .setControllerAdvice(globalExceptionHandler)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(
                        content().string("{\"success\":true,\"message\":\"Trip cancelled.\",\"data\":null}"));
    }
}
