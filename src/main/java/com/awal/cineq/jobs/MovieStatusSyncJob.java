package com.awal.cineq.jobs;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import com.awal.cineq.movie.model.Movie;
import com.awal.cineq.movie.repository.MovieRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import java.time.LocalDate;
import java.util.List;
// @Scheduled
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;



@Service
public class MovieStatusSyncJob {

 private final MovieRepository movieRepository;
 private final MongoTemplate mongoTemplate;
 private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MovieStatusSyncJob.class);
 public MovieStatusSyncJob(MovieRepository movieRepository, MongoTemplate mongoTemplate){

this.movieRepository = movieRepository;
this.mongoTemplate = mongoTemplate;

 }  

@Scheduled(cron = "1 0 0 * * *")
@SchedulerLock(name = "syncMoviesStatuses", lockAtLeastFor = "5m", lockAtMostFor = "10m")
public void syncMovieStatuses(){
    List<Movie> movies = movieRepository.findByIsActiveTrue();
    for(Movie movie : movies){
    
        LocalDate releaseDate = movie.getReleaseDate();
        String status = movie.getStatus();
        if(releaseDate != null){
            try {
                if(releaseDate.isBefore(LocalDate.now()) && !"NOW_SHOWING".equals(status)){
                    movie.setStatus("NOW_SHOWING");
                }
            } catch (Exception e) {
                log.error("Error parsing release date for movie ID {}: {}", movie.getId(), e.getMessage());
            }
        }

          boolean isAvailable = checkShowTime(movie.getId().toString());
        if(!isAvailable){
            movie.setIsActive(false); 
        }

        movieRepository.save(movie);

    }
}
private boolean checkShowTime(String movieId) {

    Query query = new Query();
    query.addCriteria(
        Criteria.where("movieId").is(movieId)
                .and("isActive").is(true)
                .andOperator(
                    new Criteria().orOperator(
                        Criteria.where("showDate").gt(LocalDate.now()),
                        Criteria.where("showDate").is(LocalDate.now())
                    )
                )
    );

    List<org.bson.Document> showtimes = mongoTemplate.find(query, org.bson.Document.class, "showtimes");

    return !showtimes.isEmpty();
}

}