package com.smartbatch360.api.site;

import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.customer.Customer;
import com.smartbatch360.api.customer.CustomerRepository;
import com.smartbatch360.api.customer.CustomerStatus;
import com.smartbatch360.api.site.dto.SiteRequest;
import com.smartbatch360.api.site.dto.SiteResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private CustomerRepository customerRepository;

    private SiteService service() {
        return new SiteService(siteRepository, customerRepository);
    }

    private Customer customerWithId(long id, String name) throws Exception {
        Customer c = new Customer();
        c.setName(name);
        c.setContactPerson("Contact");
        c.setPhone("9000000000");
        c.setStatus(CustomerStatus.ACTIVE);
        Field idField = Customer.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(c, id);
        return c;
    }

    @Test
    void createRejectsUnknownCustomer() {
        when(customerRepository.findById(5L)).thenReturn(Optional.empty());
        SiteRequest request = new SiteRequest("Kharadi", 5L, "Pune", SiteStatus.ACTIVE);

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(NotFoundException.class);

        verify(siteRepository, never()).save(any());
    }

    @Test
    void createsSiteLinkedToCustomer() throws Exception {
        Customer customer = customerWithId(5L, "SmartBatch Solutions");
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));
        when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteRequest request = new SiteRequest("Kharadi", 5L, "Pune", SiteStatus.ACTIVE);
        SiteResponse response = service().create(request);

        assertThat(response.name()).isEqualTo("Kharadi");
        assertThat(response.customerName()).isEqualTo("SmartBatch Solutions");
    }
}
