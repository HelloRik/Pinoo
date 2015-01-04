//package com.pinoo.demo.neo4j.model;
//
////import org.springframework.data.neo4j.annotation.GraphId;
////import org.springframework.data.neo4j.annotation.NodeEntity;
//
////@NodeEntity
//public class Person {
//
//    // @GraphId
//    private Long id;
//
//    // @Indexed(indexName = "name")
//    private String name;
//
//    // private int age;
//    //
//    // private String work;
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    // public int getAge() {
//    // return age;
//    // }
//    //
//    // public void setAge(int age) {
//    // this.age = age;
//    // }
//    //
//    // public String getWork() {
//    // return work;
//    // }
//    //
//    // public void setWork(String work) {
//    // this.work = work;
//    // }
//
//    // @Override
//    // public String toString() {
//    // return "Person [id=" + id + ", name=" + name + ", age=" + age + ", work="
//    // + work + "]";
//    // }
//
//}
//
//// public class Movie {
////
////
////
//// @Indexed(type = FULLTEXT, indexName = "search")
//// String title;
////
//// Person director;
////
//// @RelatedTo(type="ACTS_IN", direction = INCOMING)
//// Set<Person> actors;
////
//// @RelatedToVia(type = "RATED")
//// Iterable<Rating> ratings;
////
//// @Query("start movie=node({self}) match
//// movie-->genre<--similar return similar")
//// Iterable<Movie> similarMovies;
// // }