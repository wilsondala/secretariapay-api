package com.secretariapay.api.controller;

import com.secretariapay.api.dto.academic.AcademicServiceOrderDto;
import com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus;
import com.secretariapay.api.service.academic.AcademicServiceOrderService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcademicServiceOrderControllerQueueTest {

    @Test
    void listaPadraoDeveRemoverPedidosEntreguesDaFilaOperacional() throws Exception {
        AcademicServiceOrderService service = mock(AcademicServiceOrderService.class);
        AcademicServiceOrderController controller = new AcademicServiceOrderController(service);
        AcademicServiceOrderDto.Response paid = response(AcademicServiceOrderStatus.PAGO);
        AcademicServiceOrderDto.Response delivered = response(AcademicServiceOrderStatus.ENTREGUE);

        when(service.list(null, null)).thenReturn(List.of(delivered, paid));

        assertThat(controller.list(null, null)).containsExactly(paid);
    }

    @Test
    void filtroExplicitoEntregueDeveContinuarDisponivelParaConsulta() throws Exception {
        AcademicServiceOrderService service = mock(AcademicServiceOrderService.class);
        AcademicServiceOrderController controller = new AcademicServiceOrderController(service);
        AcademicServiceOrderDto.Response delivered = response(AcademicServiceOrderStatus.ENTREGUE);

        when(service.list(AcademicServiceOrderStatus.ENTREGUE, null)).thenReturn(List.of(delivered));

        assertThat(controller.list(AcademicServiceOrderStatus.ENTREGUE, null)).containsExactly(delivered);
    }

    private AcademicServiceOrderDto.Response response(AcademicServiceOrderStatus status) throws Exception {
        Constructor<?> constructor = AcademicServiceOrderDto.Response.class.getDeclaredConstructors()[0];
        Object[] arguments = new Object[constructor.getParameterCount()];
        arguments[2] = status;
        return (AcademicServiceOrderDto.Response) constructor.newInstance(arguments);
    }
}
