/*
 * Copyright (c) 2019 Preferred.AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.preferred.venom.job;

import ai.preferred.venom.Handler;
import ai.preferred.venom.request.VRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SchedulerTest {

  private final String url = "https://venom.preferred.ai";
  private final VRequest vRequest = new VRequest(url);
  final Handler handler = new Handler() {
    @Override
    public void tokenize() {

    }

    @Override
    public void parse() {

    }

    @Override
    public void extract() {

    }
  };

  @Test
  void testAddRequest() {
    final FIFOJobQueue jobQueue = new FIFOJobQueue();
    final Scheduler scheduler = new Scheduler(jobQueue);
    scheduler.add(vRequest);
    final Job job = jobQueue.poll();
    Assertions.assertNotNull(job);
    Assertions.assertEquals(vRequest, job.getRequest());
    Assertions.assertNull(job.getHandler());
  }

  @Test
  void testAddRequestHandler() {
    final FIFOJobQueue jobQueue = new FIFOJobQueue();
    final Scheduler scheduler = new Scheduler(jobQueue);
    scheduler.add(vRequest, handler);
    final Job job = jobQueue.poll();
    Assertions.assertNotNull(job);
    Assertions.assertEquals(vRequest, job.getRequest());
    Assertions.assertEquals(handler, job.getHandler());
  }

  @Test
  void testAddRequestJobAttribute() {
    final FIFOJobQueue jobQueue = new FIFOJobQueue();
    final Scheduler scheduler = new Scheduler(jobQueue);
    final PriorityJobAttribute priorityJobAttribute = new PriorityJobAttribute();
    scheduler.add(vRequest, priorityJobAttribute);
    final Job job = jobQueue.poll();
    Assertions.assertNotNull(job);
    Assertions.assertEquals(vRequest, job.getRequest());
    Assertions.assertNull(job.getHandler());
    Assertions.assertEquals(priorityJobAttribute, job.getJobAttribute(priorityJobAttribute.getClass()));
  }

  @Test
  void testAddRequestHandlerJobAttribute() {
    final FIFOJobQueue jobQueue = new FIFOJobQueue();
    final Scheduler scheduler = new Scheduler(jobQueue);
    final PriorityJobAttribute priorityJobAttribute = new PriorityJobAttribute();
    scheduler.add(vRequest, handler, priorityJobAttribute);
    final Job job = jobQueue.poll();
    Assertions.assertNotNull(job);
    Assertions.assertEquals(vRequest, job.getRequest());
    Assertions.assertEquals(handler, job.getHandler());
    Assertions.assertEquals(priorityJobAttribute, job.getJobAttribute(priorityJobAttribute.getClass()));
  }

}
