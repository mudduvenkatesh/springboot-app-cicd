package com.example.app.unit;

import com.example.app.model.Product;
import com.example.app.repository.ProductRepository;
import com.example.app.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void findAll_returnsList() {
        Product p = Product.builder().id(1L).name("Widget").price(BigDecimal.TEN).build();
        when(productRepository.findAll()).thenReturn(List.of(p));

        List<Product> result = productService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Widget");
    }

    @Test
    void findById_found() {
        Product p = Product.builder().id(1L).name("Widget").price(BigDecimal.TEN).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        Optional<Product> result = productService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findById_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Product> result = productService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void create_savesProduct() {
        Product p = Product.builder().name("Gadget").price(new BigDecimal("9.99")).build();
        Product saved = Product.builder().id(1L).name("Gadget").price(new BigDecimal("9.99")).build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        Product result = productService.create(p);

        assertThat(result.getId()).isEqualTo(1L);
        verify(productRepository, times(1)).save(p);
    }

    @Test
    void delete_existingProduct_returnsTrue() {
        when(productRepository.existsById(1L)).thenReturn(true);

        boolean result = productService.delete(1L);

        assertThat(result).isTrue();
        verify(productRepository).deleteById(1L);
    }

    @Test
    void delete_missingProduct_returnsFalse() {
        when(productRepository.existsById(99L)).thenReturn(false);

        boolean result = productService.delete(99L);

        assertThat(result).isFalse();
        verify(productRepository, never()).deleteById(any());
    }
}
