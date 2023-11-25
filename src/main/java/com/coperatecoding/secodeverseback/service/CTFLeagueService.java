package com.coperatecoding.secodeverseback.service;

import com.coperatecoding.secodeverseback.domain.ctf.CTFLeague;
import com.coperatecoding.secodeverseback.domain.ctf.CTFLeagueStatus;
import com.coperatecoding.secodeverseback.dto.ctf.CTFLeagueDTO;
import com.coperatecoding.secodeverseback.exception.NotFoundException;
import com.coperatecoding.secodeverseback.repository.CTFLeagueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CTFLeagueService {

    private final CTFLeagueRepository ctfLeagueRepository;

    public void makeLeague(CTFLeagueDTO.PostRequest addRequest) {

        CTFLeague ctfLeague = CTFLeague.makeCTFLeague(addRequest.getName(), addRequest.getOpenTime(), addRequest.getCloseTime(),
                addRequest.getMemberCnt(), addRequest.getNotice(), addRequest.getDescription());

        ctfLeagueRepository.save(ctfLeague);
    }


    public CTFLeagueDTO.DetailResponse getDetailLeague(Long leaguePk) throws NoSuchElementException {
        CTFLeague league = ctfLeagueRepository.findById(leaguePk)
                .orElseThrow(() -> new NotFoundException("해당하는 리그가 존재하지 않음"));

        CTFLeagueDTO.DetailResponse detailResponse = CTFLeagueDTO.DetailResponse.builder()
                .name(league.getName())
                .openTime(league.convertDate(league.getOpenTime()))
                .closeTime(league.convertDate(league.getCloseTime()))
                .memberCnt(league.getMemberCnt())
                .notice(league.getNotice())
                .description(league.getDescription())
                .status(league.checkLeagueStatus())
                .build();

        return detailResponse;
    }

    public void editCTFLeague(Long leaguePk, CTFLeagueDTO.EditRequest request) throws RuntimeException {
        CTFLeague league = ctfLeagueRepository.findById(leaguePk)
                .orElseThrow(() -> new NotFoundException("해당하는 리그가 존재하지 않음"));

        league.edit(request.getName(), request.getOpenTime(), request.getCloseTime(), request.getMemberCnt(), request.getNotice(), request.getDescription());

    }

    public CTFLeagueStatus getCTFLeagueStatus(Long leaguePk) {
        CTFLeague league = ctfLeagueRepository.findById(leaguePk)
                .orElseThrow(() -> new NotFoundException("해당하는 리그가 존재하지 않음"));

        CTFLeagueStatus ctfLeagueStatus = league.checkLeagueStatus();
        return ctfLeagueStatus;
    }

    public void deleteCTFLeague(Long leaguePk) throws RuntimeException {

        CTFLeague league = ctfLeagueRepository.findById(leaguePk)
                .orElseThrow(() -> new NotFoundException("해당하는 리그가 존재하지 않음"));

        ctfLeagueRepository.delete(league);

    }

//    public Page<CTFLeagueDTO.AllListResponse> getCTFLeagueAll() throws RuntimeException {
//
//    }
}
