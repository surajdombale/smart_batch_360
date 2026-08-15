package com.smartbatch360.api.customer;

import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.customer.dto.CustomerRequest;
import com.smartbatch360.api.customer.dto.CustomerResponse;
import com.smartbatch360.api.site.SiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SiteRepository siteRepository;

    private CustomerService service() {
        return new CustomerService(customerRepository, siteRepository);
    }

    @Test
    void createsCustomerFromRequest() {
        CustomerRequest request = new CustomerRequest("Larsen & Toubro", "Amit Sharma", "9822334455", CustomerStatus.ACTIVE);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setName(c.getName());
            return c;
        });

        CustomerResponse response = service().create(request);

        assertThat(response.name()).isEqualTo("Larsen & Toubro");
        assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRejectedWhenCustomerHasSites() {
        Customer customer = new Customer();
        customer.setName("Tata Projects");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(siteRepository.existsByCustomerId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service().delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("sites");

        verify(customerRepository, never()).delete(any());
    }

    @Test
    void deleteSucceedsWhenNoSites() {
        Customer customer = new Customer();
        customer.setName("Afcons Infrastructure");
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(siteRepository.existsByCustomerId(2L)).thenReturn(false);

        service().delete(2L);

        verify(customerRepository).delete(customer);
    }
}
