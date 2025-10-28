package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.entity.CompanyMember;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.CompanyMemberRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrCompanyServiceTest {

    @Mock
    private CompanyMemberRepository memberRepo;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HrCompanyService service;

    private CompanyMember member;
    private Company company;
    private User user;

    @BeforeEach
    void setup() {
        company = new Company();
        company.setId(10L);
        company.setName("OpenAI SG");

        member = new CompanyMember();
        member.setCompany(company);
        member.setRole("HR");

        user = new User();
        user.setId(100L);
        user.setCompany(company);
    }

    // ========== findCompanyIdByUserId ==========

    @Test
    void findCompanyIdByUserId_success() {
        when(memberRepo.findFirstByUserIdAndRole(100L, "HR"))
                .thenReturn(Optional.of(member));

        Long result = service.findCompanyIdByUserId(100L);

        assertEquals(10L, result);
        verify(memberRepo).findFirstByUserIdAndRole(100L, "HR");
    }

    @Test
    void findCompanyIdByUserId_shouldThrow_whenNotHr() {
        when(memberRepo.findFirstByUserIdAndRole(200L, "HR"))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> service.findCompanyIdByUserId(200L));
    }

    // ========== assertHrInCompany ==========

    @Test
    void assertHrInCompany_success() {
        when(memberRepo.existsByUserIdAndCompanyIdAndRole(100L, 10L, "HR"))
                .thenReturn(true);

        assertDoesNotThrow(() -> service.assertHrInCompany(100L, 10L));
        verify(memberRepo).existsByUserIdAndCompanyIdAndRole(100L, 10L, "HR");
    }

    @Test
    void assertHrInCompany_shouldThrow_whenNotBelong() {
        when(memberRepo.existsByUserIdAndCompanyIdAndRole(100L, 10L, "HR"))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.assertHrInCompany(100L, 10L));
    }

    // ========== getCompanyIdOfHr ==========

    @Test
    void getCompanyIdOfHr_success() {
        when(memberRepo.findCompanyIdByHrUserId(100L))
                .thenReturn(Optional.of(10L));

        Long result = service.getCompanyIdOfHr(100L);

        assertEquals(10L, result);
        verify(memberRepo).findCompanyIdByHrUserId(100L);
    }

    @Test
    void getCompanyIdOfHr_shouldThrow_whenEmpty() {
        when(memberRepo.findCompanyIdByHrUserId(100L))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> service.getCompanyIdOfHr(100L));
    }

    // ========== getMyCompanyName ==========

    @Test
    void getMyCompanyName_success() {
        when(userRepository.findWithCompanyById(100L))
                .thenReturn(Optional.of(user));

        String name = service.getMyCompanyName(100L);

        assertEquals("OpenAI SG", name);
        verify(userRepository).findWithCompanyById(100L);
    }

    @Test
    void getMyCompanyName_shouldThrow_whenUserNotFound() {
        when(userRepository.findWithCompanyById(200L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.getMyCompanyName(200L));
    }

    @Test
    void getMyCompanyName_shouldThrow_whenCompanyNull() {
        User noCompanyUser = new User();
        noCompanyUser.setId(300L);
        noCompanyUser.setCompany(null);
        when(userRepository.findWithCompanyById(300L))
                .thenReturn(Optional.of(noCompanyUser));

        assertThrows(EntityNotFoundException.class,
                () -> service.getMyCompanyName(300L));
    }
}
