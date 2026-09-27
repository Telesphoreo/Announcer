package me.telesphoreo.announcer;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

@SuppressWarnings("UnstableApiUsage")
public final class AnnouncerLoader implements PluginLoader
{
    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder)
    {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder("papermc", "default", "https://repo.papermc.io/repository/maven-public/").build());
        resolver.addDependency(new Dependency(new DefaultArtifact("org.tomlj:tomlj:2.1.1"), null));
        classpathBuilder.addLibrary(resolver);
    }
}
