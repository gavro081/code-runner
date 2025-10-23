package com.github.gavro081.apiserver.service;

import com.github.gavro081.apiserver.dto.CodeSubmissionDto;
import com.github.gavro081.apiserver.model.Job;
import com.github.gavro081.apiserver.model.JobStatus;
import com.github.gavro081.apiserver.model.ProgrammingLanguage;
import com.github.gavro081.apiserver.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobService {
    private JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public UUID createJob(CodeSubmissionDto codeSubmissionDto){
        ProgrammingLanguage language = codeSubmissionDto.language();
        String code = codeSubmissionDto.code();
        Job job = Job.builder()
                .language(language)
                .code(code)
                .status(JobStatus.PENDING)
                .build();

        Job savedJob = jobRepository.save(job);
        return savedJob.getId();
    }
}
