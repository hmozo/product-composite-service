package se.magnus.microservices.composite.product.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RestController;

import se.magnus.api.composite.product.ProductAggregate;
import se.magnus.api.composite.product.ProductCompositeService;
import se.magnus.api.composite.product.RecommendationSummary;
import se.magnus.api.composite.product.ReviewSummary;
import se.magnus.api.composite.product.ServiceAddresses;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

	private final ServiceUtil serviceUtil;
	private ProductCompositeIntegration integration;
	
	public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
		super();
		this.serviceUtil = serviceUtil;
		this.integration = integration;
	}
	
	
	
	@Override
	public ProductAggregate getProduct(int productId) {
		Product product= integration.getProduct(productId);
		
		List<Recommendation> recommendations= integration.getRecommendations(productId);
		
		List<Review> reviews= integration.getReviews(productId);
		
		return createProductAggregate(product, recommendations, reviews);

	}



	private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations,
			List<Review> reviews) {
		
		 // 1. Setup product info
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        // 2. Copy summary recommendation info, if available
        List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
             recommendations.stream()
                .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate()))
                .collect(Collectors.toList());
        
        // 3. Copy summary review info, if available
        List<ReviewSummary> reviewSummaries = recommendations==null?null:
        	reviews.stream()
        		.map(r->new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject()))
        		.collect(Collectors.toList());
        
        // 4. Create info regarding the involved microservices addresses
        String serviceAddress= serviceUtil.getServiceAddress();
        String productAddress = product.getServiceAddress();
        String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);

	}






}
