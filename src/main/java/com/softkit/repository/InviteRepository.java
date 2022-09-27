package com.softkit.repository;

import com.softkit.dto.InviteResponseDto;
import com.softkit.model.Invite;
import com.softkit.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InviteRepository extends JpaRepository<Invite, Integer> {

    boolean existsByEmail(String email);

    Invite findByEmail(String email);

    Page<Invite> findByIdUserOrderByDateSentInviteDescStatusDesc(Integer id_user, Pageable pageable);
}
