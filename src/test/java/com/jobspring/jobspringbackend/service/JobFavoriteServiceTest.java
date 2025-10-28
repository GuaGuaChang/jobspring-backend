package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.FavoriteJobResponse;
import com.jobspring.jobspringbackend.entity.*;
import com.jobspring.jobspringbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobFavoriteServiceTest {

    @Mock private JobFavoriteRepository favoriteRepository;
    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private JobFavoriteService service;

    private User user;
    private Job job;
    private Company company;

    @BeforeEach
    void setup() {
        company = new Company();
        company.setId(5L);
        company.setName("OpenAI SG");

        user = new User();
        user.setId(20L);
        user.setFullName("Alice");

        job = new Job();
        job.setId(10L);
        job.setTitle("Java Developer");
        job.setCompany(company);
        job.setStatus(0);
        job.setLocation("Singapore");
        job.setEmploymentType(1);
    }

    // ========= add() =========

    @Test
    void add_shouldSaveFavorite_whenNotExists() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(favoriteRepository.existsByUserAndJob(user, job)).thenReturn(false);

        service.add(20L, 10L);

        verify(favoriteRepository).save(any(JobFavorite.class));
    }

    @Test
    void add_shouldSkipSave_whenAlreadyExists() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(favoriteRepository.existsByUserAndJob(user, job)).thenReturn(true);

        service.add(20L, 10L);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void add_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.add(99L, 10L));
    }

    @Test
    void add_shouldThrow_whenJobNotFound() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.add(20L, 99L));
    }

    @Test
    void add_shouldThrow_whenJobInactive() {
        job.setStatus(1);
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        assertThrows(IllegalStateException.class, () -> service.add(20L, 10L));
    }

    // ========= remove() =========

    @Test
    void remove_shouldDeleteFavorite_whenValid() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        service.remove(20L, 10L);

        verify(favoriteRepository).deleteByUserAndJob(user, job);
    }

    @Test
    void remove_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.remove(99L, 10L));
    }

    // ========= isFavorited() =========

    @Test
    void isFavorited_shouldReturnTrue_whenExists() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(favoriteRepository.existsByUserAndJob(user, job)).thenReturn(true);

        boolean result = service.isFavorited(20L, 10L);

        assertTrue(result);
    }

    @Test
    void isFavorited_shouldReturnFalse_whenNotExists() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(favoriteRepository.existsByUserAndJob(user, job)).thenReturn(false);

        boolean result = service.isFavorited(20L, 10L);

        assertFalse(result);
    }

    // ========= list() =========

    @Test
    void list_shouldMapToFavoriteJobResponse() {
        JobFavorite fav = new JobFavorite();
        fav.setJob(job);
        fav.setCreatedAt(LocalDateTime.now());

        Page<JobFavorite> mockPage = new PageImpl<>(List.of(fav));

        when(favoriteRepository.findByUserId(eq(20L), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<FavoriteJobResponse> result =
                service.list(20L, PageRequest.of(0, 5));

        FavoriteJobResponse r = result.getContent().get(0);
        assertEquals(10L, r.getJobId());
        assertEquals("Java Developer", r.getTitle());
        assertEquals("OpenAI SG", r.getCompany());
        verify(favoriteRepository).findByUserId(eq(20L), any(Pageable.class));
    }

    // ========= countByJob() =========

    @Test
    void countByJob_shouldReturnValue() {
        when(favoriteRepository.countByJobId(10L)).thenReturn(42L);
        long result = service.countByJob(10L);
        assertEquals(42L, result);
        verify(favoriteRepository).countByJobId(10L);
    }
}
