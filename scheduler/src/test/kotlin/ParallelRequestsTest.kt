import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.cache.normalized.watch
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import test.HelloQuery
import kotlin.test.Test
import kotlin.time.TimeSource
import kotlin.time.measureTime

class ParallelRequestsTest {
    @Test
    fun test100ParallelRequests() = runBlocking {
        val apolloClient = ApolloClient.Builder()
            .dispatcher(Dispatchers.IO)
            .normalizedCache(MemoryCacheFactory())
            .serverUrl("http://localhost:8080/graphql")
            .build()

        // Launch 100 parallel requests
        val flows = (1..100).map { index ->
            flowOf(
                apolloClient
                    .query(HelloQuery())
                    .fetchPolicy(FetchPolicy.NetworkOnly)
            )
                .flatMapLatest {
                    it.watch()
                }
                .onEach { println("We got data for flow $index") }
        }
        val before = TimeSource.Monotonic.markNow()
        combine(flows) { it ->
            // Nothing here
        }.collect {
            val after = TimeSource.Monotonic.markNow()
            println("Work finished for all workers in ${after - before}")
        }
    }
}
