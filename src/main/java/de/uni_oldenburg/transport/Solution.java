package de.uni_oldenburg.transport;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The Solution represents a solution to our optimization problem.
 * It consists of multiple {@link Tour}s.
 */
public class Solution {

	private TransportNetwork transportNetwork;
	private ArrayList<Tour> truckTours = new ArrayList<>();

	/**
	 * Simple Constructor
	 *
	 * @param transportNetwork The transport network this is a solution for
	 */
	public Solution(TransportNetwork transportNetwork) {
		this.transportNetwork = transportNetwork;
	}

	/**
	 * Add a tour to this solution
	 *
	 * @param tour The tour to add
	 */
	public void addTour(Tour tour) {
		truckTours.add(tour);
	}

	/**
	 * Remove a tour from this solution
	 *
	 * @param tour The tour to remove
	 * @return true if this solution contained the tour
	 */
	public boolean removeTour(Tour tour) {
		return truckTours.remove(tour);
	}

	/**
	 * Checks if all delivery targets have been fulfilled and no
	 * @return
	 */
	public boolean isValid() {

		HashMap<Location, Integer> deliveries = new HashMap<>();

		// Check if all delivery targets have been fulfilled
		for (Tour tour : truckTours) {

			// ckeck if truck was overloaded
			if (!tour.isValid()) return false;

			for (TourDestination tourDestination : tour.getTourDestinations()) {
				if (deliveries.containsKey(tourDestination.getDestination())) {
					deliveries.put(tourDestination.getDestination(), deliveries.get(tourDestination.getDestination())+tourDestination.getUnload());
				} else {
					deliveries.put(tourDestination.getDestination(), tourDestination.getUnload());
				}
			}
		}
		for (Location location : transportNetwork.getLocations()) {
			if (deliveries.containsKey(location) && location.getAmount() != 0)
				if (location.getAmount() != deliveries.get(location)) return false;
		}
		return true;
	}

}
