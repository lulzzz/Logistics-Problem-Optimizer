package de.uni_oldenburg.transport.optimizers;

import de.uni_oldenburg.transport.*;
import de.uni_oldenburg.transport.trucks.LargeTruck;
import de.uni_oldenburg.transport.trucks.MediumTruck;
import de.uni_oldenburg.transport.trucks.SmallTruck;

import java.util.*;


/**
 * Optimizer with Savings Algorithm
 */
public class SavingsOptimizer implements Optimizer {

    Location startLocation;
    ArrayList<ArrayList<Location>> routeList = new ArrayList<>();

    @Override
    public Solution optimizeTransportNetwork(TransportNetwork transportNetwork) {
        startLocation = transportNetwork.getStartLocation();
        Solution solution = new Solution(transportNetwork);
        HashMap<Location, Integer> shortestWays = depotDistance(transportNetwork);
        LinkedHashMap<String, Integer> savings = getSavings(shortestWays, transportNetwork);
        ArrayList<ArrayList<Location>> getRoutes = getRoutes(savings, transportNetwork);
        ArrayList<Tour> tours = null;
        ArrayList<ArrayList<Tour>> allTours = new ArrayList<>();


        for (int i = 0; i < getRoutes.size(); i++) {
            ArrayList<Location> tourLocations = getRoutes.get(i);
            tours = new ArrayList<>();

            ArrayList<Location> sortedTourLocations = new ArrayList<>();

            for (Location tourDestination : tourLocations) {
                if (!sortedTourLocations.contains(tourDestination)) {
                    sortedTourLocations.add(tourDestination);
                }
            }


            int totalAmount = 0;
            for (Location tourDestination : sortedTourLocations) {
                totalAmount += tourDestination.getAmount();
            }
            int totalsAmountCopy = totalAmount;

            //selects the trucks for the route
            while (totalsAmountCopy > 0) {
                if (totalsAmountCopy > MediumTruck.CAPACITY && totalsAmountCopy <= LargeTruck.CAPACITY) {
                    tours.add(new Tour(new LargeTruck(), startLocation));
                    totalsAmountCopy -= LargeTruck.CAPACITY;
                } else if (totalsAmountCopy > SmallTruck.CAPACITY && totalsAmountCopy <= MediumTruck.CAPACITY) {
                    tours.add(new Tour(new MediumTruck(), startLocation));
                    totalsAmountCopy -= MediumTruck.CAPACITY;
                } else if (totalsAmountCopy <= SmallTruck.CAPACITY) {
                    tours.add(new Tour(new SmallTruck(), startLocation));
                    totalsAmountCopy -= SmallTruck.CAPACITY;
                } else {

                    do {
                        tours.add(new Tour(new LargeTruck(), startLocation));
                        totalsAmountCopy -= LargeTruck.CAPACITY;
                    } while (totalsAmountCopy > MediumTruck.CAPACITY);
                }
            }

            for (int j = 0; j < tours.size(); j++) {
                int amountPossible = tours.get(j).getTruck().getCapacity();
                Location startTourLocation = startLocation;
                Location emptyTruckLocation = new Location("");
                Tour tour = tours.get(j);

                int unL = 0;

                for (int k = 0; k < tourLocations.size(); k++) {
                    if (amountPossible > 0) {
                        Location actLocation = tourLocations.get(k);
                        int unload = 0;
                        int unloadAmount = 0;

                        TourDestination tourDestination = new TourDestination(actLocation, tourLocations.get(k).getAmount());
                        unload = tourDestination.getUnload();
                        if (unload <= amountPossible) {
                            tourDestination.setUnload(actLocation.getAmount());
                            tourLocations.get(k).setAmount(0);
                            unloadAmount = unload;
                        } else {
                            int restAmount = tourLocations.get(k).getAmount() - amountPossible;
                            unloadAmount = amountPossible;
                            tourDestination.setUnload(unloadAmount);
                            tourLocations.get(k).setAmount(restAmount);
                        }
                        tour.addDestination(tourDestination);
                        amountPossible -= unload;
                        emptyTruckLocation = actLocation;
                    } else if (k < tourLocations.size()) {

                        LinkedHashMap<Location, Integer> route = transportNetwork.getShortestPath(emptyTruckLocation, startLocation);

                        for (Map.Entry<Location, Integer> entry : route.entrySet()) {
                            TourDestination destNoLoad = new TourDestination(entry.getKey(), unL);
                            if (!entry.getKey().equals(emptyTruckLocation))
                                tour.addDestination(destNoLoad);
                        }
                        break;

                    }
                }
            }

            allTours.add(tours);
        }
        for (int i = 0; i < allTours.size(); i++) {
            for (int j = 0; j < allTours.get(i).size(); j++) {
                if (allTours.get(i).get(j).isValid(true))
                    solution.addTour(allTours.get(i).get(j));
            }
        }

        return solution;

    }

    /**
     * Gets the savings for the distances between the Locations and the Depot
     *
     * @param depotDistance    HashMap with the distances to the depot
     * @param transportNetwork A transport network for which the transport problem has to be optimized
     * @return
     */
    private LinkedHashMap<String, Integer> getSavings(HashMap<Location, Integer> depotDistance, TransportNetwork transportNetwork) {
        ArrayList<Location> visitedLocations = new ArrayList<>();
        Location actLocation = startLocation;
        LinkedHashMap<String, Integer> savings = new LinkedHashMap<>();

        while (transportNetwork.getNumberOfLocations() > visitedLocations.size()) {
            int distance = 0;
            Location location;
            visitedLocations.add(startLocation);
            String names = "";
            int d1 = 0;
            int d2 = 0;
            int save = 0;
            for (int i = 0; i < visitedLocations.size(); i++) {
                if (!actLocation.getNeighbouringLocations().isEmpty()) {
                    for (Map.Entry<Location, Integer> entry : actLocation.getNeighbouringLocations().entrySet()) {
                        distance = entry.getValue();
                        location = entry.getKey();

                        for (Map.Entry<Location, Integer> dis : depotDistance.entrySet()) {
                            Location locationDis = dis.getKey();

                            if (actLocation.toString().equals(locationDis.toString())) {
                                d1 = dis.getValue();
                                names += locationDis.getName();
                            }
                            if (location.toString().equals(locationDis.toString())) {
                                d2 = dis.getValue();
                                names += location.getName();
                            }
                            save = d1 + d2 - distance;
                        }

                        if (!visitedLocations.contains(location)) {
                            visitedLocations.add(location);
                            distance = 0;
                        }

                        if (!names.contains("Hamburg")) {
                            savings.put(names, save);
                        }
                        names = "";
                    }
                }
                actLocation = visitedLocations.get(i);
            }
        }
        return savings;
    }

    /**
     * Computes the routes with multiple destinations.
     *
     * @param savings          HashMap with each saving for two locations
     * @param transportNetwork A transport network for which the transport problem has to be optimized
     * @return
     */
    private ArrayList<ArrayList<Location>> getRoutes(LinkedHashMap<String, Integer> savings, TransportNetwork transportNetwork) {

        ArrayList<ArrayList<Location>> routeList = new ArrayList<>();
        ArrayList<Location> routeLocation = new ArrayList<>();
        ArrayList<Location> visitedLocations = new ArrayList<>();
        ArrayList<Location> visitedLocations3 = new ArrayList<>();
        Location[] locations = transportNetwork.getLocations();
        Location actLocation = startLocation;
        routeLocation.add(startLocation);


        ArrayList<ArrayList<Location>> routesPart1 = computeRoutes(actLocation, transportNetwork, savings, visitedLocations);
        for (ArrayList<Location> routing1 : routesPart1) {
            routeList.add(routing1);
            for (Location visited : routing1) {
                if (!visitedLocations.contains(visited)) {
                    visitedLocations3.add(visited);
                }
            }
        }

        while (visitedLocations3.size() < locations.length) {
            for (Location visitedL : locations) {
                if (!visitedLocations3.contains(visitedL)) {
                    actLocation = visitedL;
                    break;
                }
            }
            ArrayList<ArrayList<Location>> routesPart2 = computeRoutes(actLocation, transportNetwork, savings, visitedLocations);
            for (ArrayList<Location> routing2 : routesPart2) {
                if (!routeList.contains(routing2))
                    routeList.add(routing2);
                for (Location visited : routing2) {
                    if (!visitedLocations3.contains(visited)) {
                        visitedLocations3.add(visited);
                    }
                }
            }
        }

        return routeList;
    }

    /**
     * Computes the Routes between the Depot an the Locations.
     *
     * @param actLocation      The current location
     * @param transportNetwork A transport network for which the transport problem has to be optimized.
     * @param savings          LinkedHashMap with the computed savings
     * @param visitedLocation  ArrayList with the locations already visited
     * @return
     */

    private ArrayList<ArrayList<Location>> computeRoutes(Location actLocation, TransportNetwork transportNetwork, LinkedHashMap<String, Integer> savings, ArrayList<Location> visitedLocation) {

        ArrayList<Location> routeLocation = new ArrayList<>();
        ArrayList<Location> visitedLocations2 = visitedLocation;
        ArrayList<Location> locations = new ArrayList<>();
        boolean[] savingsCheck = new boolean[savings.size()];
        LinkedHashMap<String, Integer> sorted = savings;
        Location actLocation2 = actLocation;
        routeLocation.add(startLocation);
        visitedLocations2.add(startLocation);
        Object[] o = sorted.entrySet().toArray();


        //sorts the savings, begins with the largest
        Arrays.sort(o, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Map.Entry<String, Integer>) o2).getValue()
                        .compareTo(((Map.Entry<String, Integer>) o1).getValue());
            }
        });


        for (int i = 0; i < transportNetwork.getLocations().length; i++) {
            locations.add(transportNetwork.getLocations()[i]);
        }
        int k = 0;
        int checkCounter = 0;
        transportNetwork.computeShortestPaths();


        boolean saving = false;

        for (Map.Entry<String, Integer> sav : sorted.entrySet()) {
            if (sav.getKey().contains(actLocation.getName())) {
                if (!actLocation.equals(startLocation))
                    saving = true;

            }
        }

        //creates the first route
        if (!saving && !actLocation.equals(startLocation)) {
            LinkedHashMap<Location, Integer> route = transportNetwork.getShortestPath(startLocation, actLocation);
            LinkedHashMap<Location, Integer> route2 = transportNetwork.getShortestPath(actLocation, startLocation);
            ArrayList<Location> firstRoute = new ArrayList<>();

            for (Map.Entry<Location, Integer> entry : route.entrySet()) {
                if (!entry.getKey().equals(startLocation))
                    firstRoute.add(entry.getKey());
                if (!visitedLocations2.contains(entry.getKey())) {
                    visitedLocations2.add(entry.getKey());
                    actLocation = entry.getKey();
                }
            }
            for (Map.Entry<Location, Integer> entry : route2.entrySet()) {
                if (!firstRoute.contains(entry.getKey())) {
                    firstRoute.add(entry.getKey());
                }
                if (!visitedLocations2.contains(entry.getKey())) {
                    visitedLocations2.add(entry.getKey());
                    actLocation = entry.getKey();
                }
            }
            routeList.add(firstRoute);
        }

        //creates the routes after the first route is created
        while (k < 1) {
            //goes thrue the savings and creates a route if the saving contains the selected location
            for (Object e : o) {

                ArrayList<Location> savingsLocations = new ArrayList<>();
                ArrayList<Location> singleRoute = new ArrayList<>();
                ArrayList<Location> firstRoute = new ArrayList<>();
                Location destination = new Location("");
                boolean startRouting = false;

                //checks if the location has neighbours
                if (!actLocation.getNeighbouringLocations().isEmpty()) {
                    for (Map.Entry<Location, Integer> entry : actLocation2.getNeighbouringLocations().entrySet()) {
                        destination = entry.getKey();
                        if (((Map.Entry<String, Integer>) e).getKey().contains(destination.getName())) {
                            startRouting = true;

                        }
                    }
                }

                if (startRouting) {
                    //goes through the locations and adds location to the ArrayList, if saving contains the location name
                    for (Location loc : locations) {
                        if (((Map.Entry<String, Integer>) e).getKey().contains(loc.getName())) {
                            if (!savingsLocations.contains(loc)) {
                                savingsLocations.add(loc);
                            }
                        }
                    }

                    //creates the route from depot to the first location of the saving, to the second location of the saving and back to the depot
                    if (routeList.size() == 0) {
                        LinkedHashMap<Location, Integer> route = transportNetwork.getShortestPath(startLocation, savingsLocations.get(0));

                        for (Map.Entry<Location, Integer> entry : route.entrySet()) {

                            if (!entry.getKey().equals(startLocation)) {
                                if (!firstRoute.contains(entry.getKey()))
                                    firstRoute.add(entry.getKey());
                            }
                            if (!visitedLocations2.contains(entry.getKey())) {
                                visitedLocations2.add(entry.getKey());
                                actLocation = entry.getKey();
                            }
                        }

                        LinkedHashMap<Location, Integer> route2 = transportNetwork.getShortestPath(savingsLocations.get(1), startLocation);

                        for (Map.Entry<Location, Integer> entry : route2.entrySet()) {
                            firstRoute.add(entry.getKey());
                            if (!visitedLocations2.contains(entry.getKey())) {
                                visitedLocations2.add(entry.getKey());
                                actLocation = entry.getKey();
                            }
                        }

                        ArrayList<Location> checkLocations = firstRoute;
                        for (int i = 0; i < firstRoute.size() - 1; i++) {

                            if (!firstRoute.get(i).getNeighbouringLocations().containsKey(firstRoute.get(i + 1))) {
                                checkLocations.remove(firstRoute.get(i + 1));

                            }
                        }
                        routeList.add(firstRoute);
                        savingsCheck[checkCounter] = true;

                        k++;

                    } else {
                        //check if the saving is already included in a route
                        boolean checkSaving = true;
                        for (ArrayList<Location> list : routeList) {

                            if (list.contains(savingsLocations.get(0)) && list.contains(savingsLocations.get(1))) {
                                checkSaving = false;

                            } else if (list.contains(savingsLocations.get(1))) {
                                for (ArrayList<Location> list2 : routeList) {
                                    if (list2.contains(savingsLocations.get(0))) {
                                        checkSaving = false;
                                    }
                                }
                            } else if (list.contains(savingsLocations.get(0))) {
                                for (ArrayList<Location> list2 : routeList) {
                                    if (list2.contains(savingsLocations.get(1))) {
                                        checkSaving = false;
                                    }
                                }
                            } else {
                                checkSaving = true;
                            }
                        }

                        Iterator<ArrayList<Location>> iter = routeList.iterator();

                        while (iter.hasNext()) {
                            ArrayList<Location> list = iter.next();
                            if (checkSaving == true) {
                                LinkedHashMap<Location, Integer> routeLocation1 = transportNetwork.getShortestPath(startLocation, savingsLocations.get(0));
                                LinkedHashMap<Location, Integer> routeLocation2 = transportNetwork.getShortestPath(startLocation, savingsLocations.get(1));

                                //creates the route when the second location of the saving is already included in a route
                                if (list.contains(savingsLocations.get(1)) && list.size() < routeLocation2.size() - 1) {

                                    LinkedHashMap<Location, Integer> route = transportNetwork.getShortestPath(startLocation, savingsLocations.get(0));
                                    ArrayList<Location> routePart = new ArrayList<>();
                                    if (!visitedLocations2.contains(savingsLocations.get(0))) {
                                        for (Map.Entry<Location, Integer> entry : route.entrySet()) {
                                            if (!entry.getKey().equals(startLocation)) {
                                                if (!singleRoute.contains(entry.getKey())) {
                                                    singleRoute.add(entry.getKey());
                                                    actLocation = entry.getKey();
                                                }
                                                break;
                                            }

                                            routePart.add(entry.getKey());
                                            actLocation = entry.getKey();

                                            if (!visitedLocations2.contains(entry.getKey())) {
                                                visitedLocations2.add(entry.getKey());

                                            }
                                        }

                                        int count = singleRoute.size();

                                        while (count < list.size() + 1) {

                                            singleRoute.add(list.get(count - 1));
                                            if (!visitedLocations2.contains(list.get(count - 1))) {
                                                visitedLocations2.add(list.get(count - 1));
                                            }
                                            count++;
                                        }
                                        for (Location destination2 : list) {
                                            if (!singleRoute.contains(destination2)) {
                                                singleRoute.add(destination2);
                                            }
                                        }
                                        savingsCheck[checkCounter] = true;
                                        routeList.remove(list);
                                        break;
                                    }
                                } else if (list.contains(savingsLocations.get(0)) && list.size() < routeLocation1.size() - 1) {

                                    //creates the route when the first location of the saving is already included in a route
                                    LinkedHashMap<Location, Integer> route2 = transportNetwork.getShortestPath(startLocation, savingsLocations.get(1));
                                    ArrayList<Location> routePart = new ArrayList<>();
                                    if (!visitedLocations2.contains(savingsLocations.get(1))) {
                                        for (Map.Entry<Location, Integer> entry : route2.entrySet()) {
                                            if (!routePart.contains(entry.getKey())) {
                                                routePart.add(entry.getKey());
                                                actLocation = entry.getKey();
                                            }
                                            if (!visitedLocations2.contains(entry.getKey())) {
                                                visitedLocations2.add(entry.getKey());
                                            }
                                        }
                                        int count = 0;

                                        for (int i = 0; i < routePart.size(); i++) {
                                            if (!routePart.get(i).equals(startLocation)) {
                                                if (list.contains(routePart.get(i))) {
                                                    singleRoute.add(routePart.get(i));
                                                    if (!visitedLocations2.contains(list.get(i))) {
                                                        visitedLocations2.add(list.get(i));
                                                    }
                                                    count++;
                                                } else {
                                                    singleRoute.add(routePart.get(i));
                                                    count++;
                                                }
                                            }
                                        }


                                        while (count < list.size() + 1) {
                                            singleRoute.add(list.get(count - 1));
                                            count++;
                                        }
                                        routeList.remove(list);
                                        savingsCheck[checkCounter] = true;
                                        break;
                                    }
                                } else {

                                    //creates the route when the locations of the saving are not included in a route
                                    LinkedHashMap<Location, Integer> route = transportNetwork.getShortestPath(startLocation, savingsLocations.get(0));
                                    for (Map.Entry<Location, Integer> entry : route.entrySet()) {
                                        if (!entry.getKey().equals(startLocation)) {
                                            if (!singleRoute.contains(entry.getKey())) {
                                                singleRoute.add(entry.getKey());
                                                actLocation = entry.getKey();
                                            }
                                        }
                                        if (!visitedLocations2.contains(entry.getKey())) {
                                            visitedLocations2.add(entry.getKey());
                                        }
                                    }

                                    LinkedHashMap<Location, Integer> route2 = transportNetwork.getShortestPath(savingsLocations.get(1), startLocation);

                                    for (Map.Entry<Location, Integer> entry : route2.entrySet()) {
                                        if (!entry.getKey().equals(visitedLocations2.get(visitedLocations2.size() - 1))) {

                                            singleRoute.add(entry.getKey());
                                            actLocation = entry.getKey();
                                        }
                                        if (!visitedLocations2.contains(entry.getKey())) {
                                            visitedLocations2.add(entry.getKey());
                                        }
                                    }
                                    savingsCheck[checkCounter] = true;
                                    break;
                                }
                            }
                        }
                    }

                }

                //checks if the locations of the route are valid
                boolean check = false;

                for (int g = 0; g < singleRoute.size(); g++) {
                    for (Map.Entry<Location, Integer> neighbour : singleRoute.get(g).getNeighbouringLocations().entrySet()) {
                        if (g + 1 < singleRoute.size())
                            if (neighbour.getKey().equals(singleRoute.get(g + 1))) {
                                check = true;
                            }
                    }

                }

                ArrayList<Location> checkLocations = singleRoute;
                for (int i = 0; i < singleRoute.size() - 1; i++) {
                    if (!singleRoute.get(i).getNeighbouringLocations().containsKey(singleRoute.get(i + 1))) {
                        checkLocations.remove(singleRoute.get(i + 1));
                        i--;
                    }
                }

                if (singleRoute.size() != 0 && check == true) {
                    routeList.add(singleRoute);
                }
                k++;
                checkCounter++;
            }
        }


        return routeList;
    }


    /**
     * Finds out the shortest way from the startLocation to each Location.
     *
     * @param network A transport network for which the transport problem has to be optimized
     * @return
     */
    private HashMap depotDistance(TransportNetwork network) {
        HashMap<Location, Integer> depotDistanceHashMap = new HashMap<>();
        ArrayList<Location> visitedLocations = new ArrayList<>();
        Location actLocation = startLocation;
        depotDistanceHashMap.put(startLocation, 0);

        while (network.getNumberOfLocations() > visitedLocations.size()) {
            int distance = 0;
            Location location;
            visitedLocations.add(startLocation);
            network.computeShortestPaths();
            for (int i = 0; i < visitedLocations.size(); i++) {
                if (!actLocation.getNeighbouringLocations().isEmpty()) {
                    for (Map.Entry<Location, Integer> entry : actLocation.getNeighbouringLocations().entrySet()) {
                        location = entry.getKey();
                        LinkedHashMap<Location, Integer> dista = network.getShortestPath(startLocation, location);
                        for (Map.Entry<Location, Integer> d : dista.entrySet()) {
                            int dist = d.getValue();
                            distance += dist;
                        }

                        if (!depotDistanceHashMap.containsKey(location))
                            depotDistanceHashMap.put(location, distance);
                        if (!visitedLocations.contains(location)) {
                            visitedLocations.add(location);

                        }
                        distance = 0;
                    }
                }
                actLocation = visitedLocations.get(i);
            }
        }

        return depotDistanceHashMap;
    }


}