/**
 * Copyright (C) 2011  jtalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Also add information on how to contact you by electronic and paper mail.
 * Creation date: Apr 12, 2011 / 8:05:19 PM
 * The jtalks.org Project
 */

package org.jtalks.antarcticle.model.dao.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jtalks.antarcticle.model.dao.ArticleDao;
import org.jtalks.antarcticle.model.entity.Article;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

/**
 * @author Vitaliy Kravchenko
 * @author Dmitry Sokolov
 */
@ContextConfiguration(locations = {"classpath:/org/jtalks/antarcticle/model/entity/applicationContext-dao.xml"})
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = true)
@Transactional
public class ArticleHibernateDoaTest extends AbstractTransactionalTestNGSpringContextTests {
    
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private ArticleDao dao;
    private Session session;
    
    
    @BeforeMethod
    public void setUp() {
        session = sessionFactory.getCurrentSession();
        ObjectsFactory.setSession(session);
    }
    
    @Test
    public void testSave() {
        Article article = ObjectsFactory.getDefaultArticle();
        dao.saveOrUpdate(article);
        assertFalse(article.getId() == 0, "Id not created for article");
        Article sameArticle = (Article)session.get(Article.class, article.getId());
        assertReflectionEquals(article, sameArticle);
        assertEquals(article.getId(), sameArticle.getId());
        assertEquals(article.getArticleContent(), sameArticle.getArticleContent());
        assertEquals(article.getArticleTitle(), sameArticle.getArticleTitle());
        assertEquals(article.getCreationDate(), sameArticle.getCreationDate());
        assertEquals(article.getArticleCollection().getId(), sameArticle.getArticleCollection().getId());
        assertEquals(article.getUserCreated().getId(), sameArticle.getUserCreated().getId());
    }
    
    @Test
    public void testGet() {
        Article article = ObjectsFactory.getDefaultArticle();
        session.persist(article);
        assertFalse(article.getId() == 0);
        Long id = article.getId();
        Article sameArticle = dao.get(id);
        assertReflectionEquals(article, sameArticle);
    }
    
    @Test
    public void testUpdate() {
        String newContent = "Changed article content";
        Article article = ObjectsFactory.getDefaultArticle();
        session.persist(article);
        assertNotSame(article.getId(), 0);
        Long id = article.getId();
        session.evict(article);
        article = dao.get(id);
        article.setArticleContent(newContent);
        dao.saveOrUpdate(article);
        session.evict(article);
        Article sameArticle = dao.get(id);
        assertEquals(sameArticle.getArticleContent(), newContent);
    }

    @Test
    public void testGetFirstArticleFromCollection(){
        Article article = ObjectsFactory.getDefaultArticle();           
        session.save(article);         
        session.flush();
        session.evict(article);

        Article obtainedArticle = dao.getFirstArticleFromCollection(article.getArticleCollection().getId());
        assertEquals(article,obtainedArticle);
    }

    @Test
    public void testDelete() {
        int initCount = getCount();
        Article article = ObjectsFactory.getDefaultArticle();
        session.persist(article);
        Long id1 = article.getId();
        session.evict(article);
        assertEquals(getCount(), initCount+1);
        article = ObjectsFactory.getDefaultArticle();
        session.persist(article);
        Long id2 = article.getId();
        session.evict(article);
        assertEquals(getCount(), initCount+2);
        assertTrue(dao.delete(id1));
        assertTrue(dao.delete(id2));
        assertEquals(getCount(), initCount);
    }


    
    private int getCount() {
        return ((Number) session.createQuery("select count(*) from Article").uniqueResult()).intValue();
    }
}