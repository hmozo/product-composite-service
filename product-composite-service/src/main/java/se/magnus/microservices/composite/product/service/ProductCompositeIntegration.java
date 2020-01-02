package se.magnus.microservices.composite.product.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

	private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

	private final RestTemplate restTemplate;
	private final ObjectMapper mapper;

	private final String productServiceUrl;
	private final String recommendationServiceUrl;
	private final String reviewServiceUrl;

	@Autowired
	public ProductCompositeIntegration(RestTemplate restTemplate, ObjectMapper mapper,

			@Value("${app.product-service.host}") String productServiceHost,
			@Value("${app.product-service.port}") int productServicePort,

			@Value("${app.recommendation-service.host}") String recommendationServiceHost,
			@Value("${app.recommendation-service.port}") int recommendationServicePort,

			@Value("${app.review-service.host}") String reviewServiceHost,
			@Value("${app.review-service.port}") int reviewServicePort

	) {
		super();
		this.restTemplate = restTemplate;
		this.mapper = mapper;

		this.productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product";
		this.recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort
				+ "/recommendation?productId=";
		this.reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review?productId=";

	}

	@Override
	public Product getProduct(int productId) {
		
		try {
			String url= productServiceUrl + productId;
            LOG.debug("Will call getProduct API on URL: {}", url);
            
            Product product= restTemplate.getForObject(url, Product.class);
            LOG.debug("Found a product with id: {}", product.getProductId());
            
            return product;

		}catch(HttpClientErrorException exception) {
			switch(exception.getStatusCode()) {
			case NOT_FOUND:
                throw new NotFoundException(getErrorMessage(exception));
			case UNPROCESSABLE_ENTITY :
	            throw new InvalidInputException(getErrorMessage(exception));

	        default:
	            LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", exception.getStatusCode());
	            LOG.warn("Error body: {}", exception.getResponseBodyAsString());
	            throw exception;
	
			}
		}

	}
	
	private String getErrorMessage(HttpClientErrorException exception) {
		try {
			return mapper.readValue(exception.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
		}catch (IOException ioex) {
            return exception.getMessage();
        }
	}

	@Override
	public List<Recommendation> getRecommendations(int productId) {
		
		try {
			String url= recommendationServiceUrl + productId;
			List<Recommendation> recommendations= restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Recommendation>>(){}).getBody();

			 LOG.debug("Found {} recommendations for a product with id: {}", recommendations.size(), productId);
	         return recommendations;
	         
		}catch (Exception ex) {
            LOG.warn("Got an exception while requesting recommendations, return zero recommendations: {}", ex.getMessage());
            return new ArrayList<>();
        }

	}

	@Override
	public List<Review> getReviews(int productId) {
		
		 try {
	            String url = reviewServiceUrl + productId;

	            LOG.debug("Will call getReviews API on URL: {}", url);
	            List<Review> reviews = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Review>>() {}).getBody();

	            LOG.debug("Found {} reviews for a product with id: {}", reviews.size(), productId);
	            return reviews;

	        } catch (Exception ex) {
	            LOG.warn("Got an exception while requesting reviews, return zero reviews: {}", ex.getMessage());
	            return new ArrayList<>();
	        }
		 
	}

}
