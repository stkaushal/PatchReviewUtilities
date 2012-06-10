package org.ljc.adoptojdk;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class MergePatches {
	
	//this can be tuned to say how willing the program is to merging patches from different packages, higher = patches further apart in the
	//package tree will be merged
	public static int MERGIENESS = 3;
	
	public static int DIFF_MAX = 10;
	
	

	public static void main(String[] args) throws IOException {
		Path path = Paths.get(args[0]);
		new MergePatches(path);
	}

	private Path globalRoot;

	public MergePatches(Path path) throws IOException {
		globalRoot = path;
		Visitor vis = new Visitor(path);
		Files.walkFileTree(path, vis);
		
		List<List<DirTreeNode>> out = new ArrayList<>();

		//retrieve groups of diffs until the tree is empty
		while (vis.rootNode.getSize()>1) {
			List<DirTreeNode> bag = vis.rootNode.getDepthFirstGetBags();
			
			int bagSize=findListSize(bag.size());
			
			while(bag.size()>0) {
				List<DirTreeNode> newBag = new ArrayList<>(bag.subList(0, Math.min(bag.size(),bagSize)));

				bag.removeAll(newBag);
				out.add(newBag);
			}
		}
		
		printBags(out);
		
	}
	
	private void printBags(List<List<DirTreeNode>> out) {
		

		List<String> usedNames = new ArrayList<>();
		for(List<DirTreeNode> bag: out) {
			
			Path lowestCommonAnc = null;
			//Find common parent
			for(DirTreeNode file : bag) {
				if(lowestCommonAnc == null)lowestCommonAnc=file.location;
				while(!file.location.startsWith(lowestCommonAnc)) {
					lowestCommonAnc = lowestCommonAnc.getParent();
				}
			}
			
			String patchName = globalRoot.relativize(lowestCommonAnc).toString().replace("/","_")+".patch";
			
			int i=1;
			while(usedNames.contains(patchName)) {
				patchName = globalRoot.relativize(lowestCommonAnc).toString().replace("/","_")+"_"+i+".patch";
				i++;
			}
			usedNames.add(patchName);

			System.out.println("cat \\");
			for(DirTreeNode file : bag) {
				System.out.println(globalRoot.relativize(file.location)+" \\");
			}
			System.out.println(" > diffs/"+ patchName+"\n\n");
		}
		
	}

	private class Visitor extends SimpleFileVisitor<Path> {
		DirTreeNode rootNode;

		public Visitor(Path root) {
			rootNode = new DirTreeNode(root,root);
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {

			if (file.toString().endsWith(".patch")) {
				rootNode.addNode(file);
			}

			return FileVisitResult.CONTINUE;
		}
	}
	
	private static int findListSize(int n) {
		if (n < 10)
			return n;

		int min = 5;
		for (int i = 5; i < 11; i++) {
			if (n % i == 0)
				return i;

			if (n % min < n % i) {
				min = i;
			}
		}
		return min;
	}
}