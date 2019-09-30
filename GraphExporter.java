package TSN;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

class GraphExporter {
	// GraphViz Interface For Network Graph Visualization
	public boolean isDirectedGraph = true;
	public GraphExporter() {
		
	}
	//Use the following Command in cmd
	// dot -Tsvg graph.txt -o graph.svg
	// dot -Tpng graph.txt -o graph.png
	public void Export(List<Messages> messages, List<Routes> routes, String DirPath) {
		try {
			String folderpath = DirPath + "/GraphViz";
			Files.createDirectories(Paths.get(folderpath));
			PrintWriter writer = new PrintWriter(folderpath + "/graph.txt", "UTF-8");
			String indent = "  ";
			String connector;
			//String header = (g instanceof AbstractBaseGraph && !((AbstractBaseGraph<V, E>) g).isAllowingMultipleEdges()) ? DOTUtils.DONT_ALLOW_MULTIPLE_EDGES_KEYWORD + " " : "";
			String header = "";
			String graphId = "network";
			if (!DOTUtils.isValidID(graphId)) {
				writer.close();
				throw new RuntimeException(
						"Generated graph ID '" + graphId + "' is not valid with respect to the .dot language");
			}
			if (isDirectedGraph) {
				header += DOTUtils.DIRECTED_GRAPH_KEYWORD;
				connector = " " + DOTUtils.DIRECTED_GRAPH_EDGEOP + " ";
			} else {
				header += DOTUtils.UNDIRECTED_GRAPH_KEYWORD;
				connector = " " + DOTUtils.UNDIRECTED_GRAPH_EDGEOP + " ";
			}
			header += " " + graphId + " {";
			writer.println(header);
			
			for (String link : CreateEdges(routes)) {
				String[] partStrings = link.split(":");
				String source = getVertexID(partStrings[0]);
				String target = getVertexID(partStrings[1]);
				writer.print(indent + source + connector + target);
				String labelName = null;
				Map<String, String> attributes = null;
				attributes = getEdgeAttributes(partStrings[2], getMessagePriority(partStrings[2],messages));
				renderAttributes(writer, labelName, attributes);
				writer.println(";");
			}
			
			for (String vertex :Createvertexs(routes) ) {
				writer.print(indent + getVertexID(vertex));
				String labelName = null;
				Map<String, String> attributes = null;
				attributes = getVertexAttributes(vertex);
				renderAttributes(writer, labelName, attributes);

				writer.println(";");
			}
	
			writer.println("}");
			writer.flush();
		
		} catch (Exception e){
            e.printStackTrace();
        }

	}
	private int getMessagePriority(String id, List<Messages> messages) {
		for (Messages message : messages) {
			if(id.equals(String.valueOf(message.id))) {
				return message.priority;
			}
		}
		return 0;
	}
	private Map<String, String> getEdgeAttributes (String node, int pr){
		Map<String, String> map = new HashMap<String, String>();
		map.put("label", node);
		switch (pr) {
		case 0:
			map.put("color", "maroon");
			break;
		case 1:
			map.put("color", "purple");
			break;
		case 2:
			map.put("color", "red");
			break;
		case 3:
			map.put("color", "orange");
			break;
		case 4:
			map.put("color", "fuchsia");
			break;
		case 5:
			map.put("color", "pink");
			break;
		case 6:
			map.put("color", "lime");
			break;
		case 7:
			map.put("color", "yellow");
			break;

		default:
			break;
		}
		
		return map;
	}
	private Map<String, String> getVertexAttributes (String node){
		Map<String, String> map = new HashMap<String, String>();
		if(node.contains("ES")) {
			map.put("label", node);
			map.put("ratio", "fill");
			map.put("style", "filled");
			//map.put("color", "0.201 0.753 1.000");
		}else {
			map.put("label", node);
			map.put("ratio", "fill");
			map.put("style", "filled");
			map.put("color", "0.650 0.200 1.000");
		}
		return map;
	}
	private List<String> CreateEdges(List<Routes> routes){
		List<String> Links = new ArrayList<String>();
		for (Routes route : routes) {
			for (int i = 0; i < (route.nodes.size() -1); i++) {
				for (int id : route.messsageIDs) {
					String link = route.nodes.get(i) + ":" + route.nodes.get(i+1) + ":" + id;
					Links.add(link);
				}

			}
		}
		return Links;
		
	}
	private void renderAttributes(PrintWriter out, String labelName, Map<String, String> attributes) {
		if ((labelName == null) && (attributes == null)) {
			return;
		}
		out.print(" [ ");
		if ((labelName == null)) {
			labelName = attributes.get("label");
		}
		if (labelName != null) {
			if (labelName.startsWith("<<")) {
				out.print("label=" + labelName + " ");
			} else {
				out.print("label=\"" + labelName + "\" ");
			}
		}
		if (attributes != null) {
			for (Map.Entry<String, String> entry : attributes.entrySet()) {
				String name = entry.getKey();
				if (name.equals("label")) {
					// already handled by special case above
					continue;
				}
				out.print(name + "=\"" + entry.getValue() + "\" ");
			}
		}
		out.print("]");
	}
	private String getVertexID(String vertexname) {
		// use the associated id provider for an ID of the given vertex
		String idCandidate = "\"" + vertexname + "\"";

		// test if it is a valid ID
		if (DOTUtils.isValidID(idCandidate)) {
			return idCandidate;
		}

		throw new RuntimeException("Generated id '" + idCandidate + "'for vertex '"
				+ "' is not valid with respect to the .dot language");
	}
	private List<String> Createvertexs(List<Routes> routes){
		List<String> vertexeStrings = new ArrayList<String>();
		for (Routes route : routes) {
			for (String node : route.nodes) {
				if(!vertexeStrings.contains(node)) {
					vertexeStrings.add(node);
				}
			}
		}
		return vertexeStrings;
	}
}
class DOTUtils {
	/** Keyword for representing strict graphs. */
	static final String DONT_ALLOW_MULTIPLE_EDGES_KEYWORD = "strict";
	/** Keyword for directed graphs. */
	static final String DIRECTED_GRAPH_KEYWORD = "digraph";
	/** Keyword for undirected graphs. */
	static final String UNDIRECTED_GRAPH_KEYWORD = "graph";
	/** Edge operation for directed graphs. */
	static final String DIRECTED_GRAPH_EDGEOP = "->";
	/** Edge operation for undirected graphs. */
	static final String UNDIRECTED_GRAPH_EDGEOP = "--";

	// patterns for IDs
	private static final Pattern ALPHA_DIG = Pattern.compile("[a-zA-Z]+([\\w_]*)?");
	private static final Pattern DOUBLE_QUOTE = Pattern.compile("\".*\"");
	private static final Pattern DOT_NUMBER = Pattern.compile("[-]?([.][0-9]+|[0-9]+([.][0-9]*)?)");
	private static final Pattern HTML = Pattern.compile("<.*>");

	static boolean isValidID(String idCandidate) {
		return ALPHA_DIG.matcher(idCandidate).matches() || DOUBLE_QUOTE.matcher(idCandidate).matches()
				|| DOT_NUMBER.matcher(idCandidate).matches() || HTML.matcher(idCandidate).matches();
	}

}



