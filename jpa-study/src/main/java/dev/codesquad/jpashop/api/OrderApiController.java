package dev.codesquad.jpashop.api;

import dev.codesquad.jpashop.domain.Address;
import dev.codesquad.jpashop.domain.Order;
import dev.codesquad.jpashop.domain.OrderItem;
import dev.codesquad.jpashop.domain.OrderSearch;
import dev.codesquad.jpashop.domain.OrderStatus;
import dev.codesquad.jpashop.repository.OrderRepository;
import dev.codesquad.jpashop.service.OrderService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * V1. 엔티티 직접 노출 - 엔티티가 변하면 API 스펙이 변한다. - 트랜잭션 안에서 지연 로딩 필요 - 양방향 연관관계 문제 * V2. 엔티티를 조회해서 DTO로
 * 변환(fetch join 사용X) - 트랜잭션 안에서 지연 로딩 필요 V3. 엔티티를 조회해서 DTO로 변환(fetch join 사용O) - 페이징 시에는 N 부분을
 * 포기해야함(대신에 batch fetch size? 옵션 주면 N -> 1 쿼리로 변경 가능)
 * <p>
 * V4.JPA에서 DTO로 바로 조회, 컬렉션 N 조회 (1+NQuery) - 페이징 가능 V5.JPA에서 DTO로 바로 조회, 컬렉션 1 조회 최적화 버전 (1+1Query)
 * - 페이징 가능 V6. JPA에서 DTO로 바로 조회, 플랫 데이터(1Query) (1 Query) - 페이징 불가능... *
 */

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /**
     * V1. 엔티티 직접 노출 - Hibernate5Module 모듈 등록, LAZY=null 처리 * - 양방향 관계 문제 발생 -> @JsonIgnore
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll();
        for (Order order : all) {
            order.getMember().getName(); //Lazy 강제 초기화
            order.getDelivery().getAddress(); //Lazy 강제 초기환
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName()); //Lazy 강제
        }
        return all;
    }


    /**
     * V2. DTO 변환
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAll();
        List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(Collectors.toList());
        return result;
    }

    /**
     * V3. DTO 변환 - fetch join
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(Collectors.toList());
        return result;
    }

    /**
     * V3.1 엔티티를 조회해서 DTO로 변환 페이징 고려 - ToOne 관계만 우선 모두 페치 조인으로 최적화 - 컬렉션 관계는
     * hibernate.default_batch_fetch_size, @BatchSize로 최적화
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
        @RequestParam(value = "offset", defaultValue = "0") int offset,
        @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(Collectors.toList());
        return result;
    }

    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                .map(orderItem -> new OrderItemDto(orderItem)).collect(Collectors.toList());
        }
    }

    @Data
    static class OrderItemDto {

        private String itemName;//상품 명
        private int orderPrice; //주문 가격
        private int count; //주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }

    @GetMapping("/orders")
    public ResponseEntity queryDslTest(@RequestParam String memberName,
        @RequestParam OrderStatus orderStatus) {
        OrderSearch orderSearch = new OrderSearch(memberName, orderStatus);
        return ResponseEntity.ok(orderService.findOrders(orderSearch));
    }
}