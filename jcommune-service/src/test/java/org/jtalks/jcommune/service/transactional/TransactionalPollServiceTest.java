/**
 * Copyright (C) 2011  JTalks.org Team
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
 */
package org.jtalks.jcommune.service.transactional;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.jtalks.jcommune.model.dao.PollDao;
import org.jtalks.jcommune.model.dao.PollOptionDao;
import org.jtalks.jcommune.model.entity.Poll;
import org.jtalks.jcommune.model.entity.PollOption;
import org.jtalks.jcommune.service.PollService;
import org.jtalks.jcommune.service.nontransactional.SecurityService;
import org.jtalks.jcommune.service.security.AclBuilder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Anuar Nurmakanov
 */
public class TransactionalPollServiceTest {
    private PollService pollService;
    @Mock
    private PollOptionDao pollOptionDao;
    @Mock
    private PollDao pollDao;
    @Mock
    private SecurityService securityService;
    @Mock
    private AclBuilder aclBuilder;
    
    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);
        pollService = new TransactionalPollService(pollDao, pollOptionDao, securityService);
        
        Mockito.when(aclBuilder.write()).thenReturn(aclBuilder);
        Mockito.when(aclBuilder.on(Mockito.any(Poll.class))).thenReturn(aclBuilder);
        Mockito.when(securityService.grantToCurrentUser()).thenReturn(aclBuilder);
        
    }

    @Test
    public void testAddSingleVote() {
        long pollId = 1;
        long pollOptionId = 1;
        int initialVoteCount = 2;
        Poll poll = new Poll("Poll");
        poll.setId(pollId);
        PollOption option = new PollOption("Option");
        option.setVoteCount(initialVoteCount);
        poll.addPollOptions(option);

        Mockito.when(pollOptionDao.get(pollOptionId)).thenReturn(option);
        Mockito.when(pollDao.get(pollId)).thenReturn(poll);

        Poll resultPoll = pollService.addSingleVote(pollId, pollOptionId);
        PollOption resultPollOption = resultPoll.getPollOptions().get(0);

        Assert.assertEquals(resultPollOption.getVoteCount(), initialVoteCount + 1,
                "Count of votes should be increased.");
    }
    
    @Test
    public void testAddSingleVoteInInactivePoll() {
        long pollId = 1;
        long pollOptionId = 1;
        int initialVoteCount = 2;
        Poll poll = new Poll("Poll");
        poll.setEndingDate(new DateTime(1999, 1, 1, 1, 1, 1, 1));
        poll.setId(pollId);
        PollOption option = new PollOption("Option");
        option.setVoteCount(initialVoteCount);
        poll.addPollOptions(option);

        Mockito.when(pollOptionDao.get(pollOptionId)).thenReturn(option);
        Mockito.when(pollDao.get(pollId)).thenReturn(poll);

        Poll resultPoll = pollService.addSingleVote(pollId, pollOptionId);
        PollOption resultPollOption = resultPoll.getPollOptions().get(0);

        Assert.assertEquals(resultPollOption.getVoteCount(), initialVoteCount,
                "Count of votes should be increased.");
    }

    @Test
    public void testAddMultipleVotes() {
        long pollId = 1;
        List<Long> pollOptionIds = Arrays.asList(1L, 5L, 9L);
        int initialVoteCount = 4;
        Poll poll = new Poll("Poll");
        poll.setId(pollId);
        for (Long id : pollOptionIds) {
            PollOption option = new PollOption("Option:" + String.valueOf(id));
            option.setId(id);
            option.setVoteCount(initialVoteCount);
            poll.addPollOptions(option);
        }

        Mockito.when(pollDao.get(Mockito.anyLong())).thenReturn(poll);

        Poll resultPoll = pollService.addMultipleVote(pollId, pollOptionIds);

        for (PollOption option : resultPoll.getPollOptions()) {
            Assert.assertEquals(option.getVoteCount(), initialVoteCount + 1,
                    "Count of votes should be increased.");
        }
    }
    
    @Test
    public void testAddMultipleVotesInInactivePoll() {
        long pollId = 1;
        List<Long> pollOptionIds = Arrays.asList(1L, 5L, 9L);
        int initialVoteCount = 4;
        Poll poll = new Poll("Poll");
        poll.setEndingDate(new DateTime(1999, 1, 1, 1, 1, 1, 1));
        poll.setId(pollId);
        for (Long id : pollOptionIds) {
            PollOption option = new PollOption("Option:" + String.valueOf(id));
            option.setId(id);
            option.setVoteCount(initialVoteCount);
            poll.addPollOptions(option);
        }

        Mockito.when(pollDao.get(Mockito.anyLong())).thenReturn(poll);

        Poll resultPoll = pollService.addMultipleVote(pollId, pollOptionIds);

        for (PollOption option : resultPoll.getPollOptions()) {
            Assert.assertEquals(option.getVoteCount(), initialVoteCount,
                    "Count of votes should be increased.");
        }
    }
}
