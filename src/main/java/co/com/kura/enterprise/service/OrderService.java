package co.com.kura.enterprise.service;

import co.com.kura.enterprise.api.dto.CreateOrderRequest;
import co.com.kura.enterprise.api.dto.OrderResponse;
import co.com.kura.enterprise.domain.entity.*;
import co.com.kura.enterprise.domain.repository.*;
import co.com.kura.enterprise.infrastructure.PaymentProvider;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final int WALKIN_TICKET_DAYS = 15;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WalkinTicketRepository walkinTicketRepository;
    private final PaymentRepository paymentRepository;
    private final MasterServiceRepository serviceRepository;
    private final LabOfferingRepository labOfferingRepository;
    private final PaymentProvider paymentProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        WalkinTicketRepository walkinTicketRepository,
                        PaymentRepository paymentRepository,
                        MasterServiceRepository serviceRepository,
                        LabOfferingRepository labOfferingRepository,
                        PaymentProvider paymentProvider) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.walkinTicketRepository = walkinTicketRepository;
        this.paymentRepository = paymentRepository;
        this.serviceRepository = serviceRepository;
        this.labOfferingRepository = labOfferingRepository;
        this.paymentProvider = paymentProvider;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // All items must be from the same PoS (cart restriction)
        String orderNumber = generateOrderNumber();

        BigDecimal subtotal = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(request.getUserId())
                .posId(request.getPosId())
                .status("PENDING")
                .paymentMethod(request.getPaymentMethod())
                .subtotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .notes(request.getNotes())
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .guestPhone(request.getGuestPhone())
                .build();

        order = orderRepository.save(order);

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            MasterService svc = serviceRepository.findById(itemReq.getServiceId())
                    .orElseThrow(() -> new EntityNotFoundException("Service not found"));

            // Look up PoS-specific price
            BigDecimal price = labOfferingRepository.findByPosIdAndServiceId(request.getPosId(), itemReq.getServiceId())
                    .map(LabOffering::getPrice)
                    .orElse(svc.getBasePrice() != null ? svc.getBasePrice() : BigDecimal.ZERO);

            int qty = itemReq.getQuantity() > 0 ? itemReq.getQuantity() : 1;

            OrderItem item = OrderItem.builder()
                    .orderId(order.getId())
                    .serviceId(svc.getId())
                    .serviceName(svc.getName())
                    .price(price)
                    .quantity(qty)
                    .build();
            orderItemRepository.save(item);

            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(qty)));
        }

        order.setSubtotal(subtotal);
        order.setTotal(subtotal); // No tax logic yet
        order = orderRepository.save(order);

        // Generate walk-in ticket (15 days)
        WalkinTicket ticket = WalkinTicket.builder()
                .ticketCode(generateTicketCode())
                .orderId(order.getId())
                .posId(order.getPosId())
                .status("ACTIVE")
                .expiresAt(OffsetDateTime.now().plusDays(WALKIN_TICKET_DAYS))
                .build();
        walkinTicketRepository.save(ticket);

        // Handle payment
        OrderResponse.PaymentInfo paymentInfo = null;
        if ("MERCADOPAGO".equals(request.getPaymentMethod())) {
            // MOCK INTEGRATION: MercadoPago
            PaymentProvider.PaymentResult result = paymentProvider.createPreference(
                    orderNumber, subtotal, "COP", "KURA Order " + orderNumber);

            Payment payment = Payment.builder()
                    .orderId(order.getId())
                    .paymentMethod("MERCADOPAGO")
                    .externalId(result.preferenceId())
                    .status("PENDING")
                    .amount(subtotal)
                    .currency("COP")
                    .build();
            paymentRepository.save(payment);

            paymentInfo = OrderResponse.PaymentInfo.builder()
                    .preferenceId(result.preferenceId())
                    .checkoutUrl(result.checkoutUrl())
                    .status("PENDING")
                    .build();
        }

        return buildOrderResponse(order, ticket, paymentInfo);
    }

    public OrderResponse getOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderNumber));

        WalkinTicket ticket = walkinTicketRepository.findByOrderId(order.getId()).orElse(null);
        return buildOrderResponse(order, ticket, null);
    }

    public List<OrderResponse> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(o -> {
                    WalkinTicket t = walkinTicketRepository.findByOrderId(o.getId()).orElse(null);
                    return buildOrderResponse(o, t, null);
                })
                .toList();
    }

    private OrderResponse buildOrderResponse(Order order, WalkinTicket ticket, OrderResponse.PaymentInfo paymentInfo) {
        List<OrderResponse.ItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(i -> OrderResponse.ItemResponse.builder()
                        .serviceId(i.getServiceId())
                        .serviceName(i.getServiceName())
                        .price(i.getPrice())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        OrderResponse.TicketResponse ticketResponse = null;
        if (ticket != null) {
            ticketResponse = OrderResponse.TicketResponse.builder()
                    .ticketCode(ticket.getTicketCode())
                    .status(ticket.getStatus())
                    .expiresAt(ticket.getExpiresAt())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .subtotal(order.getSubtotal())
                .total(order.getTotal())
                .expiresAt(order.getExpiresAt())
                .createdAt(order.getCreatedAt())
                .items(items)
                .ticket(ticketResponse)
                .payment(paymentInfo)
                .build();
    }

    private String generateOrderNumber() {
        return "KURA-" + System.currentTimeMillis() + "-" + (secureRandom.nextInt(9000) + 1000);
    }

    private String generateTicketCode() {
        return "TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
