//package com.pinoo.demo;
//
//import org.junit.FixMethodOrder;
//import org.junit.Test;
//import org.junit.runners.MethodSorters;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.support.ClassPathXmlApplicationContext;
//
//import com.pinoo.demo.neo4j.model.Person;
//import com.pinoo.demo.neo4j.repository.PersonRepository;
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//public class Neo4jDemo {
//
//    private static Logger logger = LoggerFactory.getLogger(Neo4jDemo.class);
//
//    private static ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("neo4j.xml");
//
//    @Autowired
//    private PersonRepository personRepository = context.getBean(PersonRepository.class);
//
//    @Test
//    public void getCursorList() throws Exception {
//        System.out.println(personRepository);
//
//        Person p = new Person();
//
//        // p.setAge(27);
//        p.setName("jd");
//        // p.setWork("editor");
//
//        personRepository.save(p);
//
//        // String DB_PATH =
//        // "/Users/Rik/Desktop/workspace/neo4j-community-2.2.0-M02/data/graph.db";
//        // GraphDatabaseService graphDb = new
//        // GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
//
//        // for (Iterator<Person> it = personRepository.findAll().iterator();
//        // it.hasNext();) {
//        // Person node = it.next();
//        // System.out.println(node);
//        // }
//
//        // Transaction tx = graphDb.beginTx();
//        //
//        // Node firstNode = graphDb.createNode();
//        // firstNode.setProperty("message", "Hello, ");
//        //
//        // tx.success();
//        // tx.finish();
//    }
//
// }
