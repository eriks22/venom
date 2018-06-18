/*
 * Copyright 2017 Preferred.AI
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

import ai.preferred.venom.Handleable;
import ai.preferred.venom.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class FIFOScheduler extends AbstractQueueScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(FIFOScheduler.class);

  private final LinkedBlockingQueue<Job> queue = new LinkedBlockingQueue<>();

  @Override
  BlockingQueue<Job> getQueue() {
    return queue;
  }

  @Override
  public void add(@NotNull Request r, @NotNull Handleable h, Priority p, Priority pf) {
    Job job = new BasicJob(r, h, p, pf, queue);
    getQueue().add(job);
    LOGGER.debug("Job {} - {} added to queue.", job.toString(), r.getUrl());
  }

  @Override
  public void put(@Nonnull Job job) throws InterruptedException {
    getQueue().put(job);
  }

  @Override
  public boolean offer(Job job, long timeout, TimeUnit unit) throws InterruptedException {
    return getQueue().offer(job, timeout, unit);
  }
}
