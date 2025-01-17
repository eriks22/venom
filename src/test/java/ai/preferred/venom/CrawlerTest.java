/*
 * Copyright 2018 Preferred.AI
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

package ai.preferred.venom;

import ai.preferred.venom.fetcher.FakeFetcher;
import ai.preferred.venom.fetcher.Fetcher;
import ai.preferred.venom.job.FIFOJobQueue;
import ai.preferred.venom.job.LazyPriorityJobQueue;
import ai.preferred.venom.job.Scheduler;
import ai.preferred.venom.request.Request;
import ai.preferred.venom.request.VRequest;
import ai.preferred.venom.response.VResponse;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CrawlerTest {

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
  public void testCrawler() throws Exception {
    final LinkedList<FakeFetcher.Status> statuses = new LinkedList<>();
    statuses.add(FakeFetcher.Status.COMPLETE);
    statuses.add(FakeFetcher.Status.COMPLETE);
    statuses.add(FakeFetcher.Status.COMPLETE);

    final FakeFetcher fetcher = new FakeFetcher(statuses);
    final Handler assertHandler = new Handler() {
      @Override
      public void tokenize() {

      }

      @Override
      public void parse() {
        try {
          Assertions.assertNull(getRequest().getProxy());
          Assertions.assertEquals(url, getRequest().getUrl());
        } catch (AssertionFailedError e) {
          throw new FatalHandlerException(e);
        }

      }

      @Override
      public void extract() {

      }
    };

    try (final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxConnections(1)
        .setMaxTries(2)
        .setJobQueue(new FIFOJobQueue())
        .setSleepScheduler(new SleepScheduler(0))
        .build()
        .start()) {

      crawler.getScheduler().add(vRequest, assertHandler);
      crawler.getScheduler().add(vRequest, assertHandler);
      crawler.getScheduler().add(vRequest, assertHandler);
    }

    Assertions.assertEquals(3, fetcher.getCounter());
  }

  @Test
  public void testCrawlerStartAndClose() throws Exception {
    final LinkedList<FakeFetcher.Status> statuses = new LinkedList<>();
    statuses.add(FakeFetcher.Status.COMPLETE);
    statuses.add(FakeFetcher.Status.COMPLETE);
    statuses.add(FakeFetcher.Status.COMPLETE);

    final FakeFetcher fetcher = new FakeFetcher(statuses);

    final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxConnections(1)
        .setMaxTries(2)
        .setJobQueue(new FIFOJobQueue())
        .setSleepScheduler(new SleepScheduler(0))
        .build();

    crawler.getScheduler().add(vRequest, handler);
    crawler.getScheduler().add(vRequest, handler);
    crawler.getScheduler().add(vRequest, handler);

    crawler.startAndClose();

    Assertions.assertEquals(3, fetcher.getCounter());
  }

  @Test
  public void testRetry() throws Exception {
    final LinkedList<FakeFetcher.Status> statuses = new LinkedList<>();
    statuses.add(FakeFetcher.Status.FAILED);
    statuses.add(FakeFetcher.Status.FAILED);
    statuses.add(FakeFetcher.Status.COMPLETE);
    statuses.add(FakeFetcher.Status.COMPLETE);
    statuses.add(FakeFetcher.Status.COMPLETE);

    final FakeFetcher fetcher = new FakeFetcher(statuses);

    try (final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxConnections(1)
        .setMaxTries(5)
        .setJobQueue(new FIFOJobQueue())
        .setSleepScheduler(new SleepScheduler(0))
        .build()
        .start()) {

      crawler.getScheduler().add(vRequest, handler);
    }

    Assertions.assertEquals(3, fetcher.getCounter());
  }

  @Test
  public void testMaxTries() throws Exception {
    final LinkedList<FakeFetcher.Status> statuses = new LinkedList<>();
    statuses.add(FakeFetcher.Status.FAILED);
    statuses.add(FakeFetcher.Status.FAILED);
    statuses.add(FakeFetcher.Status.FAILED);
    statuses.add(FakeFetcher.Status.FAILED);
    statuses.add(FakeFetcher.Status.FAILED);
    statuses.add(FakeFetcher.Status.FAILED);
    statuses.add(FakeFetcher.Status.FAILED);

    final FakeFetcher fetcher = new FakeFetcher(statuses);

    try (final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxConnections(1)
        .setMaxTries(5)
        .setJobQueue(new FIFOJobQueue())
        .setSleepScheduler(new SleepScheduler(0))
        .build()
        .start()) {

      crawler.getScheduler().add(vRequest, handler);
    }

    Assertions.assertEquals(5, fetcher.getCounter());
  }

  @Test
  public void testProxyProportionRemoved() throws Exception {
    final LinkedList<FakeFetcher.Status> statuses = new LinkedList<>();
    statuses.add(FakeFetcher.Status.FAILED);
    statuses.add(FakeFetcher.Status.COMPLETE);

    final HttpHost proxy = new HttpHost("127.0.0.1:8080");
    final FakeFetcher fetcher = new FakeFetcher(statuses);
      final Handler assertHandler = new Handler() {
          @Override
          public void tokenize() {

          }

          @Override
          public void parse() {
              try {
                Assertions.assertEquals(proxy, getRequest().getProxy());
                Assertions.assertEquals(url, getRequest().getUrl());
              } catch (AssertionFailedError e) {
                  throw new FatalHandlerException(e);
              }

          }

          @Override
          public void extract() {

          }
      };

    try (final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxConnections(1)
        .setPropRetainProxy(0.2)
        .setMaxTries(5)
        .setJobQueue(new FIFOJobQueue())
        .setSleepScheduler(new SleepScheduler(0))
        .build()
        .start()) {

      final VRequest vRequestProxied = VRequest.Builder.get(url).setProxy(proxy).build();
      crawler.getScheduler().add(vRequestProxied, assertHandler);
    }

    Assertions.assertEquals(2, fetcher.getCounter());
  }

  @Test
  public void testProxyProportionRetained() throws Exception {
    final LinkedList<FakeFetcher.Status> statuses = new LinkedList<>();
    statuses.add(FakeFetcher.Status.COMPLETE);

    final HttpHost proxy = new HttpHost("127.0.0.1:8080");
    final FakeFetcher fetcher = new FakeFetcher(statuses);
      final Handler assertHandler = new Handler() {
          @Override
          public void tokenize() {

          }

          @Override
          public void parse() {
              try {
                  Assertions.assertEquals(proxy, getRequest().getProxy());
                  Assertions.assertEquals(url, getRequest().getUrl());
              } catch (AssertionFailedError e) {
                  throw new FatalHandlerException(e);
              }

          }

          @Override
          public void extract() {

          }
      };

    try (final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxConnections(1)
        .setPropRetainProxy(0.2)
        .setMaxTries(5)
        .setJobQueue(new FIFOJobQueue())
        .setSleepScheduler(new SleepScheduler(0))
        .build()
        .start()) {

      final VRequest vRequestProxied = VRequest.Builder.get(url).setProxy(proxy).build();
      crawler.getScheduler().add(vRequestProxied, assertHandler);
    }

    Assertions.assertEquals(1, fetcher.getCounter());
  }

  @Test
  public void testLazySchedulerIntegration() throws Exception {
    final LinkedList<FakeFetcher.Status> statuses = new LinkedList<>();
    statuses.add(FakeFetcher.Status.COMPLETE);
    statuses.add(FakeFetcher.Status.COMPLETE);
    statuses.add(FakeFetcher.Status.COMPLETE);
    statuses.add(FakeFetcher.Status.COMPLETE);

    final FakeFetcher fetcher = new FakeFetcher(statuses);

    final List<Request> requests = new LinkedList<>();
    requests.add(vRequest);
    requests.add(vRequest);
    requests.add(vRequest);

    try (final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxConnections(1)
        .setPropRetainProxy(0.2)
        .setMaxTries(5)
        .setJobQueue(new LazyPriorityJobQueue(requests.iterator(), handler))
        .setSleepScheduler(new SleepScheduler(0))
        .build()
        .start()) {

      crawler.getScheduler().add(vRequest, handler);
    }

    Assertions.assertEquals(4, fetcher.getCounter());
  }

  @Test
  public void testUrlRouterIntegration() throws Exception {
    final LinkedList<FakeFetcher.Status> statuses = new LinkedList<>();
    statuses.add(FakeFetcher.Status.COMPLETE);

    final FakeFetcher fetcher = new FakeFetcher(statuses);

    final UrlRouter urlRouter = new UrlRouter(handler);
    try (final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxTries(1)
        .setJobQueue(new FIFOJobQueue())
        .setSleepScheduler(new SleepScheduler(0))
        .setHandlerRouter(urlRouter)
        .build()
        .start()) {

      crawler.getScheduler().add(vRequest);
    }

    Assertions.assertEquals(1, fetcher.getCounter());
  }

  @Test
  public void testFatalHandlerException() {
    Assertions.assertThrows(FatalHandlerException.class, () -> {
      final List<FakeFetcher.Status> statuses = Arrays.asList(
          FakeFetcher.Status.COMPLETE,
          FakeFetcher.Status.COMPLETE,
          FakeFetcher.Status.COMPLETE);
      final Fetcher fetcher = new FakeFetcher(new LinkedList<>(statuses));
      try (final Crawler crawler = Crawler.builder()
          .setFetcher(fetcher)
          .setMaxTries(1)
          .setMaxConnections(1)
          .setJobQueue(new FIFOJobQueue())
          .setSleepScheduler(new SleepScheduler(0))
          .build()
          .start()) {

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

        final Handler exceptionHandler = new Handler() {
          @Override
          public void tokenize() {
          }
          @Override
          public void parse() {
              throw new FatalHandlerException();
          }
          @Override
          public void extract() {

          }
        };

        crawler.getScheduler().add(vRequest, handler);

        crawler.getScheduler().add(vRequest, exceptionHandler);

        crawler.getScheduler().add(vRequest, handler);
      }
    });
  }

  @Test
  public void testSessionIntegration() throws Exception {
    final LinkedList<FakeFetcher.Status> statuses = new LinkedList<>();
    statuses.add(FakeFetcher.Status.COMPLETE);

    final FakeFetcher fetcher = new FakeFetcher(statuses);

    final Session emptySession = Session.EMPTY_SESSION;

    final Handler assertHandler = new Handler() {
      @Override
      public void tokenize() {

      }

      @Override
      public void parse() {
        try {
          Assertions.assertEquals(emptySession, getSession());
        } catch (AssertionFailedError e) {
          throw new FatalHandlerException(e);
        }
      }

      @Override
      public void extract() {

      }
    };

    try (final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxTries(1)
        .setJobQueue(new FIFOJobQueue())
        .setSleepScheduler(new SleepScheduler(0))
        .setSession(emptySession)
        .build()
        .start()) {

      crawler.getScheduler().add(vRequest, assertHandler);
    }

    Assertions.assertEquals(1, fetcher.getCounter());
  }

  @Test
  public void testInterruptAndClose() throws Exception {
    final LinkedList<FakeFetcher.Status> statuses = new LinkedList<>();
    statuses.add(FakeFetcher.Status.COMPLETE);

    final FakeFetcher fetcher = new FakeFetcher(statuses);

    final Session emptySession = Session.EMPTY_SESSION;
    final Handler assertHandler = new Handler() {
      @Override
      public void tokenize() {

      }

      @Override
      public void parse() {
        try {
          Assertions.assertEquals(emptySession, getSession());
        } catch (AssertionFailedError e) {
          throw new FatalHandlerException(e);
        }
      }

      @Override
      public void extract() {

      }
    };

    final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxTries(1)
        .setJobQueue(new FIFOJobQueue())
        .setSession(emptySession)
        .build()
        .start();

    crawler.interruptAndClose();
    crawler.getScheduler().add(vRequest, assertHandler);

    Assertions.assertEquals(0, fetcher.getCounter());
  }

  @Test
  public void testStopCodeException() throws Exception {
    final List<FakeFetcher.Status> statuses = Arrays.asList(
        FakeFetcher.Status.COMPLETE,
        FakeFetcher.Status.STOP,
        FakeFetcher.Status.COMPLETE);

    final FakeFetcher fetcher = new FakeFetcher(new LinkedList<>(statuses));

    try (final Crawler crawler = Crawler.builder()
        .setFetcher(fetcher)
        .setMaxConnections(1)
        .setMaxTries(5)
        .setJobQueue(new FIFOJobQueue())
        .setSleepScheduler(new SleepScheduler(0))
        .build()
        .start()) {

      for (FakeFetcher.Status status : statuses) {
        crawler.getScheduler().add(vRequest, handler);
      }
    }

    Assertions.assertEquals(3, fetcher.getCounter());
  }

}
