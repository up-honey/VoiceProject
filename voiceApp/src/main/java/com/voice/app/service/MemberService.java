package com.voice.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voice.app.domain.Member;
import com.voice.app.dto.MemberAdminDTO;
import com.voice.app.dto.MemberDTO;
import com.voice.app.dto.SignupDTO;
import com.voice.app.exception.UserNotFoundException;
import com.voice.app.repository.MemberRepository;
import com.voice.app.repository.VoiceRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final VoiceRepository voiceRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Transactional
    public void deleteUser(String username) {
        try {
            Member user = memberRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
            voiceRepository.deleteAllByMemberId(user.getId());
            memberRepository.delete(user);
        } catch (Exception e) {
            log.error("Error deleting user: " + username, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }
    
    public void signupProcess(SignupDTO signupDTO) {
        String username = signupDTO.getUsername();
        if (memberRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다.");
        }

        Member member = new Member();
        member.setUsername(username);
        member.setName(signupDTO.getName());
        member.setPassword(passwordEncoder.encode(signupDTO.getPassword()));
        member.setEmail(signupDTO.getEmail());
        member.setCreateDate(LocalDateTime.now());
        
        if (username.equalsIgnoreCase("ADMIN")) {
            member.setRole("ROLE_ADMIN");
        } else {
            member.setRole("ROLE_USER");
        }

        memberRepository.save(member);
    }
    
    public boolean checkUsername(String username) {
    	return memberRepository.existsByUsername(username);
    }
    
    // 관리자가 모든 회원정보 조회
    public Page<MemberAdminDTO> getAllMembersPaged(String kw, int page, int size) {
        long startRow = (long) page * size;
        long endRow = startRow + size;
        
        List<Member> members = memberRepository.findAllWithPaginationAndKeyword(startRow, endRow, kw);
        
        long total = memberRepository.countByKeyword(kw); // 검색 결과의 전체 회원 수를 가져옵니다.
        
        return new PageImpl<>(
            members.stream().map(MemberAdminDTO::fromEntity).collect(Collectors.toList()),
            PageRequest.of(page, size),
            total
        );
    }
    
    public MemberDTO getUserInfo(String username) {
        return memberRepository.findByUsername(username)
            .map(MemberDTO::fromEntity)
            .orElseThrow(() -> new UserNotFoundException("유저 정보를 찾아올 수 없습니다"));
    }
    
    public MemberDTO updateUserInfo(String username, MemberDTO updateInfo) {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException("유저 정보를 찾아올 수 없습니다"));
        
        member.setName(updateInfo.getName());
        
        Member updatedMember = memberRepository.save(member);
        return MemberDTO.fromEntity(updatedMember);
    }
}
